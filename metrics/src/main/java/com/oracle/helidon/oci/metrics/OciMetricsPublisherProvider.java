/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.common.config.Config;
import io.helidon.metrics.api.MetricsPublisher;
import io.helidon.metrics.spi.MetricsPublisherProvider;

/**
 * Contract for OCI metrics publisher providers.
 */
public class OciMetricsPublisherProvider implements MetricsPublisherProvider {
    static final String CONFIG_KEY = "oci";

    /**
     * Returns the configuration key for the OCI metrics publisher.
     *
     * @return OCI metrics publisher config key
     */
    public String configKey() {
        return CONFIG_KEY;
    }

    /**
     * Creates an OCI metrics publisher from configuration.
     *
     * @param config publisher configuration
     * @param name configured publisher name
     * @return configured OCI metrics publisher
     */
    public MetricsPublisher create(Config config, String name) {
        return OciMetricsPublisherConfig.builder().config(config).build();
    }
}
