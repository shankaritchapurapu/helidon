/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.bmc.monitoring.MonitoringClient;

/**
 * Creates {@link Monitoring} client using injected OCI authentication support and Helidon configuration.
 * <p>
 * Reduced weight so service-provided supplier with default weight is used.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class OciMonitoringSupplier implements Supplier<Monitoring> {

    private final BasicAuthenticationDetailsProvider authProvider;
    private final Function<Monitoring, URI> metricsEndpointFactory;
    private final Config config;

    @Service.Inject
    OciMonitoringSupplier(BasicAuthenticationDetailsProvider authProvider,
                          @OciMetrics Function<Monitoring, URI> metricsEndpointFactory,
                          Config config) {
        this.authProvider = authProvider;
        this.metricsEndpointFactory = metricsEndpointFactory;
        this.config = config;
    }

    @Override
    public Monitoring get() {
        OverlayMetricReporterConfig reporterConfig = OciMetricsConfigSupport.overlayReporterConfig(config)
                .orElseThrow(() -> new IllegalStateException(
                        "OCI Monitoring client is only available for overlay metrics reporters."));
        var clientConfigurationOpt = reporterConfig.client();

        var monitoringClient = clientConfigurationOpt.map(clientConfiguration -> new MonitoringClient(
                        authProvider,
                        clientConfiguration))
                .orElseGet(() -> new MonitoringClient(authProvider));
        /*
        As per the OCI metrics doc, once the monitoring client is initialized we change the endpoint to the proper
        ingestion endpoint.
         */
        monitoringClient.setEndpoint(metricsEndpointFactory.apply(monitoringClient).toString());
        return monitoringClient;
    }

}
