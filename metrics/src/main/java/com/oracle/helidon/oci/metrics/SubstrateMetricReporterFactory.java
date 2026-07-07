/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.service.registry.Services;

import com.oracle.pic.telemetry.commons.metrics.DianogaReporter;
import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.dianoga.MetricTimeSeriesClient;

final class SubstrateMetricReporterFactory {
    private SubstrateMetricReporterFactory() {
    }

    static MetricReporter create(SubstrateMetricReporterConfigBlueprint config) {
        MetricTimeSeriesClient t2Client = config.metricTimeSeriesClient()
                .orElseGet(() -> Services.get(MetricTimeSeriesClient.class));
        return create(config, t2Client, config.metricTimeSeriesClient().isEmpty());
    }

    private static MetricReporter create(OciMetricReporterConfig config,
                                         MetricTimeSeriesClient t2Client,
                                         boolean ownsClient) {
        Optional<String> effectiveHostName = OciTelemetryRuntime.effectiveHostName(config);

        DianogaReporter.Builder builder = DianogaReporter.builder()
                .t2Client(t2Client)
                .project(config.project().orElseThrow())
                .fleet(config.fleet().orElseThrow());
        config.availabilityDomain().ifPresent(builder::availabilityDomain);
        config.faultDomain().ifPresent(builder::faultDomain);
        effectiveHostName.ifPresent(builder::hostname);

        MetricReporter reporter = builder.build();
        return applyOwnership(reporter, t2Client, ownsClient);
    }

    static MetricReporter applyOwnership(MetricReporter reporter, AutoCloseable closeable, boolean ownsClient) {
        return ownsClient ? OwnedMetricReporter.create(reporter, closeable) : reporter;
    }
}
