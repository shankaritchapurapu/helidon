/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.config.Config;

/**
 * Provider for substrate OCI metrics reporter configuration.
 */
public final class SubstrateMetricReporterConfigProvider implements OciMetricReporterConfigProvider {

    /**
     * Creates a new provider for service loading.
     */
    public SubstrateMetricReporterConfigProvider() {
    }

    @Override
    public String configKey() {
        return OciMetricReporterType.SUBSTRATE.configKey();
    }

    @Override
    public OciMetricReporterConfig create(Config config, String name) {
        return SubstrateMetricReporterConfig.builder()
                .config(config)
                .name(name)
                .build();
    }
}
