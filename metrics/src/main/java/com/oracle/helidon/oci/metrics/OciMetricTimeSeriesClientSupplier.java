/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.telemetry.dianoga.MetricTimeSeriesClient;

/**
 * Creates substrate T2 metric time-series clients using injected OCI authentication support and Helidon configuration.
 * <p>
 * Reduced weight so service-provided suppliers with default weight are used.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class OciMetricTimeSeriesClientSupplier implements Supplier<MetricTimeSeriesClient> {

    private final BasicAuthenticationDetailsProvider authProvider;
    private final Config config;
    private final BiFunction<BasicAuthenticationDetailsProvider, Optional<ClientConfiguration>, MetricTimeSeriesClient>
            clientFactory;

    @Service.Inject
    OciMetricTimeSeriesClientSupplier(BasicAuthenticationDetailsProvider authProvider, Config config) {
        this(authProvider, config, OciMetricTimeSeriesClientSupplier::createClient);
    }

    OciMetricTimeSeriesClientSupplier(
            BasicAuthenticationDetailsProvider authProvider,
            Config config,
            BiFunction<BasicAuthenticationDetailsProvider, Optional<ClientConfiguration>, MetricTimeSeriesClient>
                    clientFactory) {
        this.authProvider = authProvider;
        this.config = config;
        this.clientFactory = clientFactory;
    }

    @Override
    public MetricTimeSeriesClient get() {
        SubstrateMetricReporterConfig reporterConfig = OciMetricsConfigSupport.substrateReporterConfig(config)
                .orElseThrow(() -> new IllegalStateException(
                        "OCI MetricTimeSeriesClient is only available for substrate metrics reporters."));

        MetricTimeSeriesClient client = clientFactory.apply(authProvider, reporterConfig.client());

        if (reporterConfig.endpoint().isPresent()) {
            client.setEndpoint(reporterConfig.endpoint().orElseThrow().toString());
        } else {
            Region resolvedRegion = RegionSupport.resolve(reporterConfig.region(), () -> Services.get(Region.class));
            client.setRegion(resolvedRegion.getPublicRegionName());
        }
        return client;
    }

    private static MetricTimeSeriesClient createClient(BasicAuthenticationDetailsProvider authProvider,
                                                       Optional<ClientConfiguration> clientConfig) {
        return clientConfig
                .map(config -> new MetricTimeSeriesClient(authProvider, config))
                .orElseGet(() -> new MetricTimeSeriesClient(authProvider));
    }
}
