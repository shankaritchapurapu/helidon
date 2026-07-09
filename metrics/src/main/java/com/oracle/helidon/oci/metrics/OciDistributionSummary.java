/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.Optional;

import io.helidon.metrics.api.DistributionStatisticsConfig;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.HistogramSnapshot;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;

import com.oracle.pic.telemetry.commons.metrics.model.Observation;

/**
 * OCI-backed distribution summary.
 */
final class OciDistributionSummary extends AbstractOciMeter implements DistributionSummary {
    private final DistributionSummary delegate;
    private final OciIntervalAccumulator intervalAccumulator;

    OciDistributionSummary(Builder builder, OciMeterRegistry registry, DistributionSummary delegate, boolean enabled) {
        super(registry, delegate, enabled);
        this.delegate = delegate;
        this.intervalAccumulator = new OciIntervalAccumulator(delegate.id().name(),
                                                             registry.accumulatorConfig(),
                                                             registry.accumulatorConfig()
                                                                     .maxRawSummarySamplesPerSecond(),
                                                             registry.accumulatorStats());
    }

    static Builder builder(String name) {
        return new Builder(name);
    }

    static Builder builder(String name, DistributionStatisticsConfig.Builder configBuilder) {
        Builder builder = new Builder(name);
        builder.distributionStatisticsConfig(configBuilder);
        return builder;
    }

    @Override
    public void record(double amount) {
        if (amount < 0D) {
            throw new IllegalArgumentException("Distribution summary amount must be non-negative");
        }
        delegate.record(amount);
        if (accumulationEnabled()) {
            intervalAccumulator.record(registry().clock().wallTime(), OciUnitConverter.normalizeAmount(baseUnit(), amount));
        }
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public double totalAmount() {
        return delegate.totalAmount();
    }

    @Override
    public double mean() {
        return delegate.mean();
    }

    @Override
    public double max() {
        return delegate.max();
    }

    @Override
    public HistogramSnapshot snapshot() {
        return delegate.snapshot();
    }

    List<Observation> drainClosedIntervalSamples(long nowMillis) {
        return intervalAccumulator.drainClosed(nowMillis);
    }

    List<Observation> drainAllIntervalSamples() {
        return intervalAccumulator.drainAll();
    }

    int pendingBucketCount() {
        return intervalAccumulator.pendingBucketCount();
    }

    static final class Builder extends AbstractOciMeterBuilder<DistributionSummary.Builder, DistributionSummary>
            implements DistributionSummary.Builder {

        private Double scale;
        private DistributionStatisticsConfig.Builder distributionStatisticsConfig;
        private Boolean publishPercentileHistogram;

        Builder(String name) {
            super(name);
        }

        @Override
        public DistributionSummary.Builder scale(double scale) {
            this.scale = scale;
            return this;
        }

        @Override
        public DistributionSummary.Builder distributionStatisticsConfig(DistributionStatisticsConfig.Builder configBuilder) {
            this.distributionStatisticsConfig = configBuilder;
            return this;
        }

        @Override
        public DistributionSummary.Builder publishPercentileHistogram(boolean publishPercentileHistogram) {
            this.publishPercentileHistogram = publishPercentileHistogram;
            return this;
        }

        @Override
        public Optional<Double> scale() {
            return Optional.ofNullable(scale);
        }

        @Override
        public Optional<DistributionStatisticsConfig.Builder> distributionStatisticsConfig() {
            return Optional.ofNullable(distributionStatisticsConfig);
        }

        @Override
        public Optional<Boolean> publishPercentileHistogram() {
            return Optional.ofNullable(publishPercentileHistogram);
        }

        @Override
        Meter.Type meterType() {
            return Meter.Type.DISTRIBUTION_SUMMARY;
        }

        @Override
        DistributionSummary.Builder createDelegateBuilder(MetricsFactory metricsFactory) {
            DistributionSummary.Builder delegateBuilder =
                    metricsFactory
                            .distributionSummaryBuilder(name(),
                                                        distributionStatisticsConfig()
                                                                .orElseGet(
                                                                        metricsFactory::distributionStatisticsConfigBuilder));
            configureDelegate(delegateBuilder);
            scale().ifPresent(delegateBuilder::scale);
            publishPercentileHistogram().ifPresent(delegateBuilder::publishPercentileHistogram);
            distributionStatisticsConfig().ifPresent(delegateBuilder::distributionStatisticsConfig);
            return delegateBuilder;
        }

        @Override
        DistributionSummary build(boolean enabled, OciMeterRegistry registry, Meter delegate) {
            return new OciDistributionSummary(this, registry, (DistributionSummary) delegate, enabled);
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
