/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
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

    private final FunctionalCounter delegate;
    private final AtomicLong lastSampledCount = new AtomicLong();
    private final ReentrantLock samplingLock = new ReentrantLock();

    OciFunctionalCounter(Builder<T> builder,
                         OciMeterRegistry registry,
                         FunctionalCounter delegate,
                         boolean enabled,
                         boolean accumulationEligible) {
        super(registry, delegate, enabled, accumulationEligible);
        this.delegate = delegate;
    }

    static <T> Builder<T> builder(String name, T stateObject, Function<T, Long> fn) {
        return new Builder<>(name, stateObject, fn);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    OptionalLong deltaIfChanged() {
        return drainDeltaIfChanged().positiveDelta();
    }

    Sample drainDeltaIfChanged() {
        long value = count();
        while (true) {
            long previous = lastSampledCount.get();
            if (value < previous) {
                if (lastSampledCount.compareAndSet(previous, value)) {
                    return Sample.empty(value);
                }
                continue;
            }
            long delta = value - previous;
            if (delta == 0L) {
                return Sample.empty(value);
            }
            if (lastSampledCount.compareAndSet(previous, value)) {
                return new Sample(previous, value, delta);
            }
        }
    }

    void restoreDelta(Sample sample) {
        sample.positiveDelta()
                .ifPresent(ignored -> {
                    if (!lastSampledCount.compareAndSet(sample.sampledCount(), sample.previousCount())) {
                        throw new IllegalStateException("Functional counter sample state changed during restore");
                    }
                });
    }

    void lockSampling() {
        samplingLock.lock();
    }

    void unlockSampling() {
        samplingLock.unlock();
    }

    record Sample(long previousCount, long sampledCount, long delta) {
        static Sample empty(long sampledCount) {
            return new Sample(sampledCount, sampledCount, 0L);
        }

        OptionalLong positiveDelta() {
            return delta > 0L ? OptionalLong.of(delta) : OptionalLong.empty();
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
        FunctionalCounter build(boolean enabled,
                                boolean accumulationEligible,
                                OciMeterRegistry registry,
                                Meter delegate) {
            return new OciFunctionalCounter<>(this,
                                             registry,
                                             (FunctionalCounter) delegate,
                                             enabled,
                                             accumulationEligible);
        }

        @Override
        Builder<T> self() {
            return this;
        }
    }
}
