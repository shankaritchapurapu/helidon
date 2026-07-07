/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.service.registry.Services;

import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporterBuilder;

final class OverlayMetricReporterFactory {
    private OverlayMetricReporterFactory() {
    }

    static MetricReporter create(OverlayMetricReporterConfigBlueprint config) {
        Monitoring monitoringClient = config.monitoring().orElseGet(() -> Services.get(Monitoring.class));
        Region resolvedRegion = RegionSupport.resolve(config.region(), () -> Services.get(Region.class));
        String publicRegionName = resolvedRegion.getPublicRegionName();
        Optional<String> effectiveHostName = OciTelemetryRuntime.effectiveHostName(config);

        TelemetryReporterBuilder builder = new TelemetryReporterBuilder()
                .monitoringClient(monitoringClient)
                .project(config.project().orElseThrow())
                .fleet(config.fleet().orElseThrow())
                .postMetricsRequestHeaders(config.requestHeaders());
        effectiveHostName.ifPresent(builder::hostname);
        config.useMetadataService().ifPresent(builder::useMetadataService);
        config.overrideMetricKeys().ifPresent(builder::shouldOverrideMetricKeys);
        config.availabilityDomain().ifPresent(builder::availabilityDomain);
        config.faultDomain().ifPresent(builder::faultDomain);
        builder.region(publicRegionName);

        MetricReporter reporter = builder.build();
        return applyOwnership(reporter, monitoringClient, config.monitoring().isEmpty());
    }

    static MetricReporter applyOwnership(MetricReporter reporter, AutoCloseable closeable, boolean ownsClient) {
        return ownsClient ? OwnedMetricReporter.create(reporter, closeable) : reporter;
    }
}
