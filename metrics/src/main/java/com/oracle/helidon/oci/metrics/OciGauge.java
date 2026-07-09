/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;

/**
 * OCI-backed gauge that computes its value on access.
 *
 * @param <N> sampled number type
 */
final class OciGauge<N extends Number> extends AbstractOciMeter implements Gauge<N> {

    private final Gauge<N> delegate;
    private final Supplier<N> supplier;

    OciGauge(Builder<N> builder, OciMeterRegistry registry, Gauge<N> delegate, boolean enabled) {
        super(registry, delegate, enabled);
        this.delegate = delegate;
        this.supplier = builder.supplier();
    }

    static <N extends Number> Builder<N> builder(String name, Supplier<N> supplier) {
        return new Builder<>(name, supplier);
    }

    static <T> Builder<Double> builder(String name, T instance, ToDoubleFunction<T> fn) {
        return new Builder<>(name, () -> fn.applyAsDouble(instance));
    }

    @Override
    public N value() {
        delegate.value();
        return supplier.get();
    }

    static final class Builder<N extends Number> extends AbstractOciMeterBuilder<Gauge.Builder<N>, Gauge<N>>
            implements Gauge.Builder<N> {

        private final Supplier<N> supplier;

        Builder(String name, Supplier<N> supplier) {
            super(name);
            this.supplier = supplier;
        }

        @Override
        public Supplier<N> supplier() {
            return supplier;
        }

        @Override
        Meter.Type meterType() {
            return Meter.Type.GAUGE;
        }

        @Override
        Gauge.Builder<N> createDelegateBuilder(MetricsFactory metricsFactory) {
            return configureDelegate(metricsFactory.gaugeBuilder(name(), supplier));
        }

        @Override
        Gauge<N> build(boolean enabled, OciMeterRegistry registry, Meter delegate) {
            return new OciGauge<>(this, registry, (Gauge<N>) delegate, enabled);
        }

        @Override
        Builder<N> self() {
            return this;
        }
    }
}
