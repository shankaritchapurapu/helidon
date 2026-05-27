/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.helidon.config.AbstractConfigSource;
import io.helidon.config.ConfigException;
import io.helidon.config.spi.ConfigContent;
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigNode.ObjectNode;
import io.helidon.config.spi.EventConfigSource;
import io.helidon.config.spi.LazyConfigSource;
import io.helidon.config.spi.NodeConfigSource;
import io.helidon.service.registry.Service;

import static java.lang.System.Logger.Level.WARNING;

/**
 * Lazy SSv2-backed Helidon SE config source with internal polling.
 * Only keys that were already requested through this source are represented in
 * snapshots and change events.
 */
@Service.Singleton
@Service.Named(SecretServiceConfigSource.TYPE)
public final class SecretServiceConfigSource extends AbstractConfigSource
        implements LazyConfigSource, EventConfigSource, NodeConfigSource, AutoCloseable {

    static final String TYPE = "oci-secret-service";
    static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(5);
    static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMinutes(30);

    private static final System.Logger LOGGER = System.getLogger(SecretServiceConfigSource.class.getName());

    private final String prefix;
    private final Duration cacheTtl;
    private final Duration pollInterval;
    private final Clock clock;
    private final Function<String, Optional<String>> resolver;
    private final AutoCloseable resolverCloseable;
    private final ScheduledExecutorService scheduler;
    private final boolean ownsScheduler;
    private final CopyOnWriteArrayList<BiConsumer<String, ConfigNode>> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, TrackedSecret> trackedSecrets = new ConcurrentHashMap<>();
    private final Lock stateLock = new ReentrantLock();

    private PublicationState publicationState = PublicationState.IDLE;
    private java.util.concurrent.ScheduledFuture<?> pollingTask;
    private Map<String, String> emittedSnapshotValues;

    @Service.Inject
    SecretServiceConfigSource(SecretServiceConfigSourceFactory factory) {
        this(factory.builder());
    }

    SecretServiceConfigSource(SecretServiceConfigSourceBuilder builder) {
        super(builder);
        SecretServiceConfigSourceConfig sourceConfig = builder.sourceConfig();
        this.prefix = sourceConfig.prefix();
        this.cacheTtl = sourceConfig.cacheTtl();
        this.pollInterval = sourceConfig.effectivePollInterval();
        this.clock = builder.clock();
        this.resolver = builder.resolver();
        this.resolverCloseable = builder.resolverCloseable();
        this.scheduler = builder.scheduler();
        this.ownsScheduler = builder.ownsScheduler();
    }

    static SecretServiceConfigSourceBuilder builder() {
        return new SecretServiceConfigSourceBuilder();
    }

    @Override
    protected String uid() {
        return prefix;
    }

    @Override
    public Optional<ConfigContent.NodeContent> load() throws ConfigException {
        return Optional.of(ConfigContent.NodeContent.builder()
                                   .node(snapshot().node())
                                   .build());
    }

    @Override
    public Optional<ConfigNode> node(String key) {
        ParsedKey parsedKey = ParsedKey.parse(prefix, key);
        if (parsedKey == null) {
            return Optional.empty();
        }

        TrackedSecret created = new TrackedSecret(parsedKey.path(), clock, cacheTtl, resolver);
        TrackedSecret trackedSecret = trackedSecrets.putIfAbsent(parsedKey.configKey(), created);
        if (trackedSecret == null) {
            trackedSecret = created;
        }

        TrackedSecret.CurrentNode currentNode = trackedSecret.currentNode();
        if (currentNode.changed()) {
            requestPublish();
        }
        return currentNode.node();
    }

    @Override
    public void onChange(BiConsumer<String, ConfigNode> changedNode) {
        BiConsumer<String, ConfigNode> listener = Objects.requireNonNull(changedNode, "changedNode");
        stateLock.lock();
        try {
            if (publicationState == PublicationState.CLOSED) {
                return;
            }
            if (emittedSnapshotValues == null) {
                emittedSnapshotValues = snapshot().values();
            }
            listeners.add(listener);
            startPollingLocked();
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public void close() {
        java.util.concurrent.ScheduledFuture<?> future;
        stateLock.lock();
        try {
            if (publicationState == PublicationState.CLOSED) {
                return;
            }
            publicationState = PublicationState.CLOSED;
            future = pollingTask;
            pollingTask = null;
        } finally {
            stateLock.unlock();
        }
        if (future != null) {
            future.cancel(true);
        }
        if (ownsScheduler) {
            scheduler.shutdownNow();
        }
        closeResolver();
    }

    private void closeResolver() {
        if (resolverCloseable == null) {
            return;
        }
        try {
            resolverCloseable.close();
        } catch (Exception e) {
            LOGGER.log(WARNING, "Secret Service resolver close failed", e);
        }
    }

    private void startPollingLocked() {
        if (listeners.isEmpty() || pollingTask != null) {
            return;
        }

        long pollDelayNanos = pollInterval.toNanos();
        pollingTask = scheduler.scheduleWithFixedDelay(this::pollTrackedSecrets,
                                                       pollDelayNanos,
                                                       pollDelayNanos,
                                                       TimeUnit.NANOSECONDS);
    }

    private void pollTrackedSecrets() {
        try {
            if (trackedSecrets.isEmpty()) {
                return;
            }

            boolean changed = false;
            Instant now = clock.instant();
            for (TrackedSecret trackedSecret : trackedSecrets.values()) {
                changed |= trackedSecret.refresh(now, true);
            }

            if (!changed) {
                return;
            }

            requestPublish();
        } catch (RuntimeException e) {
            LOGGER.log(WARNING, "Secret Service polling failed", e);
        }
    }

    private Snapshot snapshot() {
        Map<String, String> values = new TreeMap<>();
        trackedSecrets.forEach((configKey, trackedSecret) -> trackedSecret.snapshotValue()
                .ifPresent(value -> values.put(configKey, value)));

        ObjectNode.Builder builder = ObjectNode.builder();
        values.forEach(builder::addValue);
        return new Snapshot(Map.copyOf(values), builder.build());
    }

    private void requestPublish() {
        boolean schedule = false;
        stateLock.lock();
        try {
            if (listeners.isEmpty()) {
                return;
            }
            if (emittedSnapshotValues == null) {
                emittedSnapshotValues = snapshot().values();
                return;
            }
            schedule = switch (publicationState) {
            case IDLE -> {
                publicationState = PublicationState.QUEUED;
                yield true;
            }
            case QUEUED, RUNNING_QUEUED, CLOSED -> false;
            case RUNNING -> {
                publicationState = PublicationState.RUNNING_QUEUED;
                yield false;
            }
            };
        } finally {
            stateLock.unlock();
        }
        if (schedule) {
            schedulePublishLoop();
        }
    }

    private void schedulePublishLoop() {
        stateLock.lock();
        try {
            if (publicationState != PublicationState.QUEUED) {
                return;
            }
        } finally {
            stateLock.unlock();
        }

        try {
            scheduler.execute(this::drainPublishQueue);
        } catch (RuntimeException e) {
            boolean closed;
            stateLock.lock();
            try {
                closed = publicationState == PublicationState.CLOSED;
                if (publicationState == PublicationState.QUEUED) {
                    publicationState = PublicationState.IDLE;
                }
            } finally {
                stateLock.unlock();
            }
            if (!closed) {
                LOGGER.log(WARNING, "Secret Service change publication scheduling failed", e);
            }
        }
    }

    private void drainPublishQueue() {
        stateLock.lock();
        try {
            if (publicationState == PublicationState.CLOSED) {
                return;
            }
            if (publicationState != PublicationState.QUEUED) {
                return;
            }
            publicationState = PublicationState.RUNNING;
        } finally {
            stateLock.unlock();
        }

        while (true) {
            Snapshot snapshot = snapshot();
            boolean changed;

            stateLock.lock();
            try {
                if (publicationState == PublicationState.CLOSED) {
                    return;
                }
                changed = !snapshot.values().equals(emittedSnapshotValues);
                if (changed) {
                    emittedSnapshotValues = snapshot.values();
                }
            } finally {
                stateLock.unlock();
            }

            if (changed) {
                notifyListeners(snapshot.node());
            }

            stateLock.lock();
            try {
                if (publicationState == PublicationState.CLOSED) {
                    return;
                }
                if (publicationState == PublicationState.RUNNING_QUEUED) {
                    publicationState = PublicationState.RUNNING;
                    continue;
                }
                publicationState = PublicationState.IDLE;
                return;
            } finally {
                stateLock.unlock();
            }
        }
    }

    private void notifyListeners(ObjectNode snapshot) {
        for (BiConsumer<String, ConfigNode> listener : listeners) {
            try {
                listener.accept("", snapshot);
            } catch (RuntimeException e) {
                LOGGER.log(WARNING, "Secret Service change listener failed", e);
            }
        }
    }

    private enum PublicationState {
        IDLE,
        QUEUED,
        RUNNING,
        RUNNING_QUEUED,
        CLOSED
    }

    private record Snapshot(Map<String, String> values, ObjectNode node) {
    }
}
