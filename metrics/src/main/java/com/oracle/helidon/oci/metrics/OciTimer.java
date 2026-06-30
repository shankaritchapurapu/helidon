/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.HistogramSnapshot;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Timer;

/**
 * OCI-backed timer with local one-minute rate state.
 */
final class OciTimer extends AbstractOciMeter implements Timer {

    private final Clock clock;
    private final Timer delegate;
    private final OciOneMinuteRate oneMinuteRate;

    OciTimer(Builder builder, OciMeterRegistry registry, Timer delegate, boolean enabled) {
        super(registry, delegate, enabled);
        this.clock = registry.clock();
        this.delegate = delegate;
        this.oneMinuteRate = new OciOneMinuteRate(clock);
    }

    static Builder builder(String name) {
        return new Builder(name);
    }

    static Timer.Sample start() {
        return start(Clock.system());
    }

    static Timer.Sample start(Clock clock) {
        long startTick = clock.monotonicTime();
        return timer -> {
            long elapsedNanos = clock.monotonicTime() - startTick;
            timer.record(elapsedNanos, TimeUnit.NANOSECONDS);
            return elapsedNanos;
        };
    }

    static Timer.Sample start(io.helidon.metrics.api.MeterRegistry registry) {
        return start(registry.clock());
    }

    @Override
    public HistogramSnapshot snapshot() {
        return delegate.snapshot();
    }

    @Override
    public void record(long amount, TimeUnit unit) {
        if (amount < 0) {
            throw new IllegalArgumentException("Timer amount must be non-negative");
        }
        delegate.record(amount, unit);
        oneMinuteRate.mark();
    }

    @Override
    public void record(Duration duration) {
        record(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public <T> T record(Supplier<T> supplier) {
        long start = clock.monotonicTime();
        try {
            return supplier.get();
        } finally {
            record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public <T> T record(Callable<T> callable) throws Exception {
        long start = clock.monotonicTime();
        try {
            return callable.call();
        } finally {
            record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void record(Runnable runnable) {
        long start = clock.monotonicTime();
        try {
            runnable.run();
        } finally {
            record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return () -> record(runnable);
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        return () -> record(callable);
    }

    @Override
    public <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> record(supplier);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        return delegate.totalTime(unit);
    }

    @Override
    public double mean(TimeUnit unit) {
        return delegate.mean(unit);
    }

    @Override
    public double max(TimeUnit unit) {
        return delegate.max(unit);
    }

    double oneMinuteRate() {
        return oneMinuteRate.rate();
    }

    static final class Builder extends AbstractOciMeterBuilder<Timer.Builder, Timer> implements Timer.Builder {

        private List<Double> percentiles = List.of();
        private List<Duration> buckets = List.of();
        private Duration minimumExpectedValue;
        private Duration maximumExpectedValue;
        private Boolean publishPercentileHistogram;

        Builder(String name) {
            super(name);
        }

        @Override
        public Timer.Builder baseUnit(String baseUnit) {
            super.baseUnit(baseUnit);
            return this;
        }

        @Override
        public Timer.Builder percentiles(double... percentiles) {
            this.percentiles = java.util.Arrays.stream(percentiles).boxed().toList();
            return this;
        }

        @Override
        public Timer.Builder buckets(Duration... buckets) {
            this.buckets = List.of(buckets);
            return this;
        }

        @Override
        public Timer.Builder minimumExpectedValue(Duration duration) {
            this.minimumExpectedValue = duration;
            return this;
        }

        @Override
        public Timer.Builder maximumExpectedValue(Duration duration) {
            this.maximumExpectedValue = duration;
            return this;
        }

        @Override
        public Timer.Builder publishPercentileHistogram(boolean publishPercentileHistogram) {
            this.publishPercentileHistogram = publishPercentileHistogram;
            return this;
        }

        @Override
        public Iterable<Double> percentiles() {
            return percentiles;
        }

        @Override
        public Iterable<Duration> buckets() {
            return buckets;
        }

        @Override
        public Optional<Duration> minimumExpectedValue() {
            return Optional.ofNullable(minimumExpectedValue);
        }

        @Override
        public Optional<Duration> maximumExpectedValue() {
            return Optional.ofNullable(maximumExpectedValue);
        }

        @Override
        public Optional<Boolean> publishPercentileHistogram() {
            return Optional.ofNullable(publishPercentileHistogram);
        }

        @Override
        Meter.Type meterType() {
            return Meter.Type.TIMER;
        }

        @Override
        Timer.Builder createDelegateBuilder(MetricsFactory metricsFactory) {
            Timer.Builder delegateBuilder = configureDelegate(metricsFactory.timerBuilder(name()));
            if (!percentiles.isEmpty()) {
                double[] configuredPercentiles = percentiles.stream().mapToDouble(Double::doubleValue).toArray();
                delegateBuilder.percentiles(configuredPercentiles);
            }
            delegateBuilder.buckets(buckets.toArray(Duration[]::new));
            minimumExpectedValue().ifPresent(delegateBuilder::minimumExpectedValue);
            maximumExpectedValue().ifPresent(delegateBuilder::maximumExpectedValue);
            publishPercentileHistogram().ifPresent(delegateBuilder::publishPercentileHistogram);
            return delegateBuilder;
        }

        @Override
        Timer build(boolean enabled, OciMeterRegistry registry, Meter delegate) {
            return new OciTimer(this, registry, (Timer) delegate, enabled);
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
