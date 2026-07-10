/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;

/**
 * Shared builder support for OCI-backed neutral meters.
 *
 * @param <B> builder type
 * @param <M> meter type
 */
abstract class AbstractOciMeterBuilder<B extends Meter.Builder<B, M>, M extends Meter> implements Meter.Builder<B, M> {

    private final String name;
    private final Map<String, String> tags = new LinkedHashMap<>();
    private String description;
    private String baseUnit;
    private String scope;

    AbstractOciMeterBuilder(String name) {
        this.name = name;
    }

    @Override
    public B tags(Iterable<Tag> tags) {
        this.tags.clear();
        tags.forEach(this::addTag);
        return self();
    }

    @Override
    public B addTag(Tag tag) {
        tags.put(tag.key(), tag.value());
        return self();
    }

    @Override
    public B description(String description) {
        this.description = description;
        return self();
    }

    @Override
    public B baseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
        return self();
    }

    @Override
    public B scope(String scope) {
        this.scope = scope;
        return self();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<String, String> tags() {
        return Map.copyOf(tags);
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<String> baseUnit() {
        return Optional.ofNullable(baseUnit);
    }

    @Override
    public Optional<String> scope() {
        return Optional.ofNullable(scope);
    }

    @Override
    public <R> R unwrap(Class<? extends R> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new IllegalArgumentException("Unsupported unwrap type: " + type.getName());
    }

    OciMeterRegistry.MeterKey key() {
        return OciMeterRegistry.MeterKey.create(name, tags, scope, meterType());
    }

    <DB extends Meter.Builder<DB, ?>> DB configureDelegate(DB delegateBuilder) {
        tags.forEach((key, value) -> delegateBuilder.addTag(Tag.create(key, value)));
        description().ifPresent(delegateBuilder::description);
        baseUnit().ifPresent(delegateBuilder::baseUnit);
        scope().ifPresent(delegateBuilder::scope);
        return delegateBuilder;
    }

    abstract Meter.Type meterType();

    abstract Meter.Builder<?, ?> createDelegateBuilder(MetricsFactory metricsFactory);

    abstract M build(boolean enabled, boolean accumulationEligible, OciMeterRegistry registry, Meter delegate);

    abstract B self();
}
