/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for bounded metric accumulators used by event-driven meters.
 */
@Prototype.Blueprint(decorator = OciMetricsAccumulatorConfigSupport.class)
@Prototype.Configured(value = "accumulators", root = false)
interface OciMetricsAccumulatorConfigBlueprint {

    /**
     * Maximum number of pending per-second buckets retained by one meter.
     *
     * @return maximum pending seconds
     */
    @Option.Configured
    @Option.DefaultInt(10)
    int maxPendingSeconds();

    /**
     * Maximum exact timer samples retained per second before compaction.
     *
     * @return maximum raw timer samples per second
     */
    @Option.Configured
    @Option.DefaultInt(1024)
    int maxRawTimerSamplesPerSecond();

    /**
     * Maximum exact distribution summary samples retained per second before compaction.
     *
     * @return maximum raw summary samples per second
     */
    @Option.Configured
    @Option.DefaultInt(1024)
    int maxRawSummarySamplesPerSecond();

    /**
     * Minimum time between accumulator pressure log messages.
     *
     * @return pressure log interval
     */
    @Option.Configured
    @Option.Default("PT30S")
    Duration pressureLogInterval();
}
