/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.concurrent.atomic.LongAdder;

import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;

/**
 * OCI-backed counter with local current-count state.
 */
final class OciCounter extends AbstractOciMeter implements Counter {
    private final Counter delegate;
    private final LongAdder pendingDelta = new LongAdder();

    OciCounter(Builder builder,
               OciMeterRegistry registry,
               Counter delegate,
               boolean enabled,
               boolean accumulationEligible) {
        super(registry, delegate, enabled, accumulationEligible);
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
        if (accumulationEnabled()) {
            pendingDelta.add(amount);
        }
    }

    @Override
    public long count() {
        return delegate.count();
    }

    long drainDelta() {
        return pendingDelta.sumThenReset();
    }

    void restoreDelta(long amount) {
        if (amount > 0L) {
            pendingDelta.add(amount);
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
        Counter build(boolean enabled, boolean accumulationEligible, OciMeterRegistry registry, Meter delegate) {
            return new OciCounter(this, registry, (Counter) delegate, enabled, accumulationEligible);
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
