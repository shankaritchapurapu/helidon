/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.helidon.metrics.api.FunctionalCounter;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;

/**
 * OCI-backed functional counter that samples its value on access.
 *
 * @param <T> state object type
 */
final class OciFunctionalCounter<T> extends AbstractOciMeter implements FunctionalCounter {

    private static final Object NO_REPORTED_VALUE = new Object();

    private final FunctionalCounter delegate;
    private final AtomicReference<Object> lastReportedValue = new AtomicReference<>(NO_REPORTED_VALUE);

    OciFunctionalCounter(Builder<T> builder, OciMeterRegistry registry, FunctionalCounter delegate, boolean enabled) {
        super(registry, delegate, enabled);
        this.delegate = delegate;
    }

    static <T> Builder<T> builder(String name, T stateObject, Function<T, Long> fn) {
        return new Builder<>(name, stateObject, fn);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    Optional<Long> valueIfChanged() {
        long value = count();
        while (true) {
            Object previous = lastReportedValue.get();
            if (previous != NO_REPORTED_VALUE && Objects.equals(previous, value)) {
                return Optional.empty();
            }
            if (lastReportedValue.compareAndSet(previous, value)) {
                return Optional.of(value);
            }
        }
    }

    static final class Builder<T> extends AbstractOciMeterBuilder<FunctionalCounter.Builder<T>, FunctionalCounter>
            implements FunctionalCounter.Builder<T> {

        private final T stateObject;
        private final Function<T, Long> fn;

        Builder(String name, T stateObject, Function<T, Long> fn) {
            super(name);
            this.stateObject = stateObject;
            this.fn = fn;
        }

        @Override
        public T stateObject() {
            return stateObject;
        }

        @Override
        public Function<T, Long> fn() {
            return fn;
        }

        @Override
        Meter.Type meterType() {
            return Meter.Type.OTHER;
        }

        @Override
        FunctionalCounter.Builder<T> createDelegateBuilder(MetricsFactory metricsFactory) {
            return configureDelegate(metricsFactory.functionalCounterBuilder(name(), stateObject, fn));
        }

        @Override
        FunctionalCounter build(boolean enabled, OciMeterRegistry registry, Meter delegate) {
            return new OciFunctionalCounter<>(this, registry, (FunctionalCounter) delegate, enabled);
        }

        @Override
        Builder<T> self() {
            return this;
        }
    }
}
