/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Map;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.pic.telemetry.commons.metrics.MetricReporter;

/**
 * Overlay OCI metrics reporter configuration.
 */
@Prototype.Blueprint(decorator = OciMetricReporterConfigSupport.class)
@Prototype.Configured(value = "overlay", root = false)
@Prototype.Provides(OciMetricReporterConfigProvider.class)
interface OverlayMetricReporterConfigBlueprint extends OciMetricReporterConfigBaseBlueprint, OciMetricReporterConfig {

    /**
     * Reporter type.
     *
     * @return reporter type
     */
    @Override
    @Option.Configured
    @Option.Default("overlay")
    String type();

    /**
     * Reporter name.
     *
     * @return reporter name
     */
    @Option.Default("overlay")
    String name();

    /**
     * Optional prebuilt monitoring client.
     *
     * @return optional monitoring client
     */
    Optional<Monitoring> monitoring();

    /**
     * Additional headers to send with OCI monitoring requests.
     *
     * @return request headers
     */
    @Option.Configured
    Map<String, String> requestHeaders();

    /**
     * Whether to use the OCI metadata service when preparing telemetry.
     *
     * @return optional metadata-service flag
     */
    @Option.Configured
    Optional<Boolean> useMetadataService();

    /**
     * Whether OCI metric keys should be overridden.
     *
     * @return optional override flag
     */
    @Option.Configured
    Optional<Boolean> overrideMetricKeys();

    @Override
    default MetricReporter createReporter() {
        return DeferredMetricReporter.create(() -> OverlayMetricReporterFactory.create(this));
    }
}
