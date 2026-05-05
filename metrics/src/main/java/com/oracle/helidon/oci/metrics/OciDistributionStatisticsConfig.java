/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.helidon.metrics.api.DistributionStatisticsConfig;

/**
 * Phase-1 distribution statistics config for the OCI metrics provider.
 * <p>
 * Snapshot-related behavior is not implemented in phase 1, but the builder is
 * still needed so the provider supports the neutral metrics API shape for
 * distribution summaries.
 * </p>
 */
final class OciDistributionStatisticsConfig implements DistributionStatisticsConfig {

    private final Optional<Iterable<Double>> percentiles;
    private final Optional<Double> minimumExpectedValue;
    private final Optional<Double> maximumExpectedValue;
    private final Optional<Iterable<Double>> buckets;

    private OciDistributionStatisticsConfig(Builder builder) {
        percentiles = optionalCopy(builder.percentiles);
        minimumExpectedValue = Optional.ofNullable(builder.minimumExpectedValue);
        maximumExpectedValue = Optional.ofNullable(builder.maximumExpectedValue);
        buckets = optionalCopy(builder.buckets);
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<Iterable<Double>> percentiles() {
        return percentiles;
    }

    @Override
    public Optional<Double> minimumExpectedValue() {
        return minimumExpectedValue;
    }

    @Override
    public Optional<Double> maximumExpectedValue() {
        return maximumExpectedValue;
    }

    @Override
    public Optional<Iterable<Double>> buckets() {
        return buckets;
    }

    @Override
    public <R> R unwrap(Class<? extends R> c) {
        if (c.isInstance(this)) {
            return c.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + c.getName());
    }

    private static Optional<Iterable<Double>> optionalCopy(List<Double> values) {
        return values == null ? Optional.empty() : Optional.of(List.copyOf(values));
    }

    static final class Builder implements DistributionStatisticsConfig.Builder {

        private Double minimumExpectedValue;
        private Double maximumExpectedValue;
        private List<Double> percentiles;
        private List<Double> buckets;

        @Override
        public OciDistributionStatisticsConfig build() {
            return new OciDistributionStatisticsConfig(this);
        }

        @Override
        public Builder minimumExpectedValue(Double min) {
            minimumExpectedValue = min;
            return this;
        }

        @Override
        public Builder maximumExpectedValue(Double max) {
            maximumExpectedValue = max;
            return this;
        }

        @Override
        public Builder percentiles(double... percentiles) {
            this.percentiles = copy(percentiles);
            return this;
        }

        @Override
        public Builder percentiles(Iterable<Double> percentiles) {
            this.percentiles = copy(percentiles);
            return this;
        }

        @Override
        public Builder buckets(double... buckets) {
            this.buckets = copy(buckets);
            return this;
        }

        @Override
        public Builder buckets(Iterable<Double> buckets) {
            this.buckets = copy(buckets);
            return this;
        }

        @Override
        public Optional<Double> minimumExpectedValue() {
            return Optional.ofNullable(minimumExpectedValue);
        }

        @Override
        public Optional<Double> maximumExpectedValue() {
            return Optional.ofNullable(maximumExpectedValue);
        }

        @Override
        public Iterable<Double> percentiles() {
            return percentiles == null ? List.of() : List.copyOf(percentiles);
        }

        @Override
        public Iterable<Double> buckets() {
            return buckets == null ? List.of() : List.copyOf(buckets);
        }

        @Override
        public <R> R unwrap(Class<? extends R> c) {
            if (c.isInstance(this)) {
                return c.cast(this);
            }
            throw new IllegalArgumentException("Cannot unwrap to " + c.getName());
        }

        private static List<Double> copy(double... values) {
            List<Double> result = new ArrayList<>(values.length);
            for (double value : values) {
                result.add(value);
            }
            return result;
        }

        private static List<Double> copy(Iterable<Double> values) {
            List<Double> result = new ArrayList<>();
            for (Double value : values) {
                result.add(value);
            }
            return result;
        }
    }
}
