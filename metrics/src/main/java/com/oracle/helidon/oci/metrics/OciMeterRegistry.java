/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;

/**
 * Meter registry for the OCI metrics provider.
 */
final class OciMeterRegistry implements MeterRegistry {

    private final Map<MeterKey, Meter> meters = new ConcurrentHashMap<>();
    private final Set<MeterKey> deleted = ConcurrentHashMap.newKeySet();
    private final MetricsConfig metricsConfig;
    private final Clock clock;
    private final OciMetricsPublisher publisher;
    private final MetricsFactory delegateFactory;
    private final MeterRegistry delegateRegistry;
    private final CopyOnWriteArrayList<Consumer<Meter>> onAddListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Meter>> onRemoveListeners = new CopyOnWriteArrayList<>();

    OciMeterRegistry(MetricsConfig metricsConfig,
                     Clock clock,
                     Consumer<Meter> onAdd,
                     Consumer<Meter> onRemove,
                     OciMetricsPublisher publisher,
                     MetricsFactory delegateFactory,
                     MeterRegistry delegateRegistry) {
        this.metricsConfig = metricsConfig;
        this.clock = clock;
        this.publisher = publisher;
        this.delegateFactory = delegateFactory;
        this.delegateRegistry = delegateRegistry;
        this.onAddListeners.add(onAdd);
        this.onRemoveListeners.add(onRemove);
    }

    OciMetricsPublisher publisher() {
        return publisher;
    }

    OciMetricsAccumulatorConfig accumulatorConfig() {
        OciMetricsPublisherConfig prototype = publisher.prototype();
        return prototype == null ? OciMetricsAccumulatorConfig.create() : prototype.accumulators();
    }

    OciAccumulatorStats accumulatorStats() {
        return publisher.accumulatorStats();
    }

    Collection<OciGauge<?>> gauges() {
        return meters(OciGauge.class);
    }

    Collection<OciCounter> counters() {
        return meters(OciCounter.class);
    }

    Collection<OciTimer> timers() {
        return meters(OciTimer.class);
    }

    Collection<OciDistributionSummary> distributionSummaries() {
        return meters(OciDistributionSummary.class);
    }

    Collection<OciFunctionalCounter<?>> functionalCounters() {
        return meters(OciFunctionalCounter.class);
    }

    @Override
    public List<Meter> meters() {
        return List.copyOf(meters.values());
    }

    @Override
    public Collection<Meter> meters(Predicate<Meter> predicate) {
        return meters.values().stream().filter(predicate).toList();
    }

    @SuppressWarnings("unchecked")
    private <T extends Meter> Collection<T> meters(Class<?> type) {
        List<T> result = new ArrayList<>();
        meters.values().forEach(meter -> {
            if (type.isInstance(meter)) {
                result.add((T) meter);
            }
        });
        return List.copyOf(result);
    }

    @Override
    public Iterable<Meter> meters(Iterable<String> scopes) {
        Set<String> allowedScopes = ConcurrentHashMap.newKeySet();
        scopes.forEach(allowedScopes::add);
        return meters.values().stream()
                .filter(meter -> meter.scope().map(allowedScopes::contains).orElse(false))
                .toList();
    }

    @Override
    public Iterable<String> scopes() {
        return meters.values().stream()
                .map(Meter::scope)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }

    @Override
    public void close() {
        meters.clear();
        deleted.clear();
    }

    @Override
    public boolean isMeterEnabled(String name, Map<String, String> tags, Optional<String> scope) {
        return metricsConfig.isMeterEnabled(name, scope.orElse(Meter.Scope.DEFAULT));
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends Meter.Builder<B, M>, M extends Meter> M getOrCreate(B builder) {
        if (!(builder instanceof AbstractOciMeterBuilder<?, ?> ociBuilder)) {
            throw new IllegalArgumentException("Unsupported builder type: " + builder.getClass().getName());
        }
        MeterKey key = ociBuilder.key();
        return (M) meters.computeIfAbsent(key, ignored -> {
            boolean enabled = isMeterEnabled(builder.name(), builder.tags(), builder.scope());
            @SuppressWarnings("rawtypes")
            Meter.Builder delegateBuilder = ociBuilder.createDelegateBuilder(delegateFactory);
            @SuppressWarnings("unchecked")
            Meter delegate = delegateRegistry.getOrCreate(delegateBuilder);
            boolean accumulationEligible = enabled
                    && publisher.enabled()
                    && publisher.shouldCreateValueMeter(builder.name(), ociBuilder.meterType());
            Meter meter = ociBuilder.build(enabled, accumulationEligible, this, delegate);
            onAddListeners.forEach(listener -> listener.accept(meter));
            return meter;
        });
    }

    @Override
    public <M extends Meter> Optional<M> meter(Class<M> meterClass, String name, Iterable<Tag> tags) {
        Map<String, String> requestedTags = tagsToMap(tags);
        return meters.entrySet().stream()
                .filter(entry -> entry.getKey().name().equals(name))
                .filter(entry -> entry.getKey().tags().equals(requestedTags))
                .map(Map.Entry::getValue)
                .filter(meterClass::isInstance)
                .map(meterClass::cast)
                .findFirst();
    }

    @Override
    public Optional<Meter> remove(Meter meter) {
        return removeInternal(meter);
    }

    @Override
    public Optional<Meter> remove(Meter.Id meterId) {
        return remove(meterId, null);
    }

    @Override
    public Optional<Meter> remove(Meter.Id meterId, String scope) {
        return meters.entrySet().stream()
                .filter(entry -> entry.getKey().name().equals(meterId.name()))
                .filter(entry -> entry.getKey().tags().equals(meterId.tagsMap()))
                .filter(entry -> entry.getKey().scope().equals(Optional.ofNullable(scope)))
                .map(Map.Entry::getValue)
                .findFirst()
                .flatMap(this::removeInternal);
    }

    @Override
    public Optional<Meter> remove(String name, Iterable<Tag> tags) {
        return remove(name, tags, null);
    }

    @Override
    public Optional<Meter> remove(String name, Iterable<Tag> tags, String scope) {
        return meters.values().stream()
                .filter(meter -> internalId(meter).map(id -> id.name().equals(name)).orElse(false))
                .filter(meter -> internalId(meter).map(id -> id.tags().equals(tagsToMap(tags))).orElse(false))
                .filter(meter -> meter.scope().equals(Optional.ofNullable(scope)))
                .findFirst()
                .flatMap(this::removeInternal);
    }

    @Override
    public boolean isDeleted(Meter meter) {
        return internalId(meter)
                .map(deleted::contains)
                .orElseGet(() -> deleted.contains(keyOf(meter)));
    }

    @Override
    public MeterRegistry onMeterAdded(Consumer<Meter> consumer) {
        onAddListeners.add(consumer);
        return this;
    }

    @Override
    public MeterRegistry onMeterRemoved(Consumer<Meter> consumer) {
        onRemoveListeners.add(consumer);
        return this;
    }

    @Override
    public <R> R unwrap(Class<? extends R> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        if (type.isInstance(delegateRegistry)) {
            return type.cast(delegateRegistry);
        }
        return delegateRegistry.unwrap(type);
    }

    private static Map<String, String> tagsToMap(Iterable<Tag> tags) {
        Map<String, String> result = new LinkedHashMap<>();
        tags.forEach(tag -> result.put(tag.key(), tag.value()));
        return result;
    }

    private static MeterKey keyOf(Meter meter) {
        return MeterKey.create(meter.id().name(), meter.id().tagsMap(), meter.scope().orElse(null), meter.type());
    }

    private Optional<Meter> removeInternal(Meter meter) {
        Optional<MeterKey> id = internalId(meter);
        Meter removed = id.map(meters::remove).orElse(null);
        if (removed != null) {
            delegateRegistry.remove(((AbstractOciMeter) removed).delegate());
            deleted.add(id.orElseThrow());
            onRemoveListeners.forEach(listener -> listener.accept(removed));
        }
        return Optional.ofNullable(removed);
    }

    private Optional<MeterKey> internalId(Meter meter) {
        return meters.entrySet().stream()
                .filter(entry -> entry.getValue() == meter)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    record MeterKey(String name, Map<String, String> tags, Optional<String> scope, Meter.Type type) {

        MeterKey {
            Objects.requireNonNull(name, "name");
            tags = Map.copyOf(new LinkedHashMap<>(tags));
            scope = Objects.requireNonNull(scope, "scope");
            type = Objects.requireNonNull(type, "type");
        }

        static MeterKey create(String name, Map<String, String> tags, String scope, Meter.Type type) {
            return new MeterKey(name, tags, Optional.ofNullable(scope), type);
        }
    }
}
