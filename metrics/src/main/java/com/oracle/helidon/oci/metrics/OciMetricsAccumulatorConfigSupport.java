/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;

import io.helidon.builder.api.Prototype;
import io.helidon.common.Errors;

final class OciMetricsAccumulatorConfigSupport
        implements Prototype.BuilderDecorator<OciMetricsAccumulatorConfig.BuilderBase<?, ?>> {

    OciMetricsAccumulatorConfigSupport() {
    }

    @Override
    public void decorate(OciMetricsAccumulatorConfig.BuilderBase<?, ?> builder) {
        Errors.Collector errors = Errors.collector();
        requirePositive(errors, "metrics.publishers[].accumulators.max-pending-seconds", builder.maxPendingSeconds());
        requirePositive(errors,
                        "metrics.publishers[].accumulators.max-raw-timer-samples-per-second",
                        builder.maxRawTimerSamplesPerSecond());
        requirePositive(errors,
                        "metrics.publishers[].accumulators.max-raw-summary-samples-per-second",
                        builder.maxRawSummarySamplesPerSecond());
        requirePositive(errors,
                        "metrics.publishers[].accumulators.pressure-log-interval",
                        builder.pressureLogInterval());
        errors.collect().checkValid();
    }

    private static void requirePositive(Errors.Collector errors, String key, int value) {
        if (value <= 0) {
            errors.fatal(key + " must be greater than zero.");
        }
    }

    private static void requirePositive(Errors.Collector errors, String key, Duration value) {
        if (value.isZero() || value.isNegative()) {
            errors.fatal(key + " must be greater than PT0S.");
        }
    }
}
