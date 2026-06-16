/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import io.helidon.config.spi.ConfigNode;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

final class TrackedSecret {
    private static final System.Logger LOGGER = System.getLogger(TrackedSecret.class.getName());

    private final String path;
    private final Clock clock;
    private final Duration cacheTtl;
    private final Function<String, Optional<String>> resolver;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private Optional<String> value = Optional.empty();
    private boolean initialized;
    private Instant expiresAt = Instant.EPOCH;

    TrackedSecret(String path,
                  Clock clock,
                  Duration cacheTtl,
                  Function<String, Optional<String>> resolver) {
        this.path = path;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
        this.resolver = resolver;
    }

    CurrentNode currentNode() {
        ValueState current = currentValue(clock.instant(), false);
        return new CurrentNode(current.value().map(ConfigNode.ValueNode::create), current.changed());
    }

    Optional<String> snapshotValue() {
        readLock.lock();
        try {
            if (!initialized) {
                return Optional.empty();
            }
            return value;
        } finally {
            readLock.unlock();
        }
    }

    boolean refresh(Instant now, boolean force) {
        return currentValue(now, force).changed();
    }

    private ValueState currentValue(Instant now, boolean force) {
        readLock.lock();
        try {
            if (!force && expiresAt.isAfter(now)) {
                return new ValueState(value, false);
            }
        } finally {
            readLock.unlock();
        }

        writeLock.lock();
        try {
            return refreshValueLocked(now, force);
        } finally {
            writeLock.unlock();
        }
    }

    private ValueState refreshValueLocked(Instant now, boolean force) {
        if (!force && expiresAt.isAfter(now)) {
            return new ValueState(value, false);
        }

        try {
            Optional<String> resolved = Optional.ofNullable(resolver.apply(path))
                    .orElse(Optional.empty());
            boolean changed = !initialized || !Objects.equals(value, resolved);
            this.value = resolved;
            this.initialized = true;
            this.expiresAt = now.plus(cacheTtl);
            if (LOGGER.isLoggable(DEBUG)) {
                LOGGER.log(DEBUG, "Resolved SSv2 path {0}; changed={1}", path, changed);
            }
            return new ValueState(value, changed);
        } catch (RuntimeException e) {
            if (initialized) {
                this.expiresAt = now.plus(cacheTtl);
                LOGGER.log(WARNING, "Failed to refresh SSv2 path " + path + "; keeping cached value", e);
                return new ValueState(value, false);
            }

            this.expiresAt = now.plus(cacheTtl);
            LOGGER.log(WARNING, "Failed to read SSv2 path " + path + "; returning empty value until cache TTL expires", e);
            return new ValueState(value, false);
        }
    }

    record CurrentNode(Optional<ConfigNode> node, boolean changed) {
    }

    private record ValueState(Optional<String> value, boolean changed) {
    }
}
