/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.config.Config;

/**
 * Provider for overlay OCI metrics reporter configuration.
 */
public final class OverlayMetricReporterConfigProvider implements OciMetricReporterConfigProvider {

    /**
     * Creates a new provider for service loading.
     */
    public OverlayMetricReporterConfigProvider() {
    }

    @Override
    public String configKey() {
        return OciMetricReporterType.OVERLAY.configKey();
    }

    @Override
    public OciMetricReporterConfig create(Config config, String name) {
        return OverlayMetricReporterConfig.builder()
                .config(config)
                .name(name)
                .build();
    }
}
