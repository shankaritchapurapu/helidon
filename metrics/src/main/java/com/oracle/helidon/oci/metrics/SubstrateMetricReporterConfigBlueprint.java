/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.dianoga.MetricTimeSeriesClient;

/**
 * Substrate OCI metrics reporter configuration.
 */
@Prototype.Blueprint(decorator = OciMetricReporterConfigSupport.class)
@Prototype.Configured(value = "substrate", root = false)
@Prototype.Provides(OciMetricReporterConfigProvider.class)
interface SubstrateMetricReporterConfigBlueprint extends OciMetricReporterConfigBaseBlueprint, OciMetricReporterConfig {

    /**
     * Reporter type.
     *
     * @return reporter type
     */
    @Override
    @Option.Configured
    @Option.Default("substrate")
    String type();

    /**
     * Reporter name.
     *
     * @return reporter name
     */
    @Option.Default("substrate")
    String name();

    /**
     * Optional prebuilt substrate metric time-series client.
     *
     * @return optional metric time-series client
     */
    Optional<MetricTimeSeriesClient> metricTimeSeriesClient();

    @Override
    default MetricReporter createReporter() {
        return DeferredMetricReporter.create(() -> SubstrateMetricReporterFactory.create(this));
    }
}
