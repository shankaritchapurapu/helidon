/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.URI;
import java.util.function.Function;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.bmc.monitoring.Monitoring;

/**
 * Supplies the T2 metrics endpoint for OCI metrics.
 * <p>
 * Consuming applications or libraries can provide their own higher-weight {@link OciMetrics}-qualified implementation
 * to send data to a different backend endpoint.
 */
@OciMetrics
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 100)
class T2MetricsEndpointFactory implements Function<Monitoring, URI> {

    private final Config config;

    @Service.Inject
    T2MetricsEndpointFactory(Config config) {
        this.config = config;
    }

    @Override
    public URI apply(Monitoring monitoring) {
        return OciMetricsConfigSupport.overlayReporterConfig(config)
                .flatMap(OverlayMetricReporterConfig::endpoint)
                .orElseGet(() -> monitoringEndpoint(monitoring));
    }

    private static URI monitoringEndpoint(Monitoring monitoring) {
        String endpoint = monitoring.getEndpoint();
        return URI.create(endpoint.contains("telemetry-ingestion.")
                                  ? endpoint
                                  : endpoint.replaceFirst("telemetry\\.", "telemetry-ingestion."));
    }
}
