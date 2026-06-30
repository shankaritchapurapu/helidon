/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;

import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;

/**
 * OCI-backed counter with local current-count state.
 */
final class OciCounter extends AbstractOciMeter implements Counter {
    private static final System.Logger LOGGER = System.getLogger(OciCounter.class.getName());

    private final Counter delegate;
    private final AtomicLong lastSampledCount = new AtomicLong();

    OciCounter(Builder builder, OciMeterRegistry registry, Counter delegate, boolean enabled) {
        super(registry, delegate, enabled);
        this.delegate = delegate;
    }

    static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public void increment() {
        increment(1L);
    }

    @Override
    public void increment(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Counter increment amount must be non-negative");
        }
        delegate.increment(amount);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    OptionalLong deltaIfChanged() {
        while (true) {
            long current = count();
            long previous = lastSampledCount.get();
            if (current < previous) {
                if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                    LOGGER.log(System.Logger.Level.DEBUG,
                               "Counter sample baseline reset because current count is less than previous sample; "
                                       + "meter={0}, previous={1}, current={2}",
                               id().name(),
                               previous,
                               current);
                }
                if (lastSampledCount.compareAndSet(previous, current)) {
                    return OptionalLong.empty();
                }
                continue;
            }
            long delta = current - previous;
            if (delta == 0L) {
                return OptionalLong.empty();
            }
            if (lastSampledCount.compareAndSet(previous, current)) {
                return OptionalLong.of(delta);
            }
        }
    }

    static final class Builder extends AbstractOciMeterBuilder<Counter.Builder, Counter> implements Counter.Builder {

        Builder(String name) {
            super(name);
        }

        @Override
        Meter.Type meterType() {
            return Meter.Type.COUNTER;
        }

        @Override
        Counter.Builder createDelegateBuilder(MetricsFactory metricsFactory) {
            return configureDelegate(metricsFactory.counterBuilder(name()));
        }

        @Override
        Counter build(boolean enabled, OciMeterRegistry registry, Meter delegate) {
            return new OciCounter(this, registry, (Counter) delegate, enabled);
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
