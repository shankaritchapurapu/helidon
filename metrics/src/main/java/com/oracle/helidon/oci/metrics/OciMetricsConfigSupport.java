/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.metrics.api.MetricsConfig;

final class OciMetricsConfigSupport {
    private OciMetricsConfigSupport() {
    }

    static OciMetricsPublisher ociPublisher(Config config) {
        return MetricsConfig.create(config.get(MetricsConfig.METRICS_CONFIG_KEY)).publishers().stream()
                .filter(OciMetricsPublisher.class::isInstance)
                .map(OciMetricsPublisher.class::cast)
                .findFirst()
                .orElseGet(() -> OciMetricsPublisher.builder().build());
    }

    static Optional<OverlayMetricReporterConfig> overlayReporterConfig(Config config) {
        OciMetricReporterConfig reporterConfig = ociPublisher(config).prototype().reporterConfig();
        return reporterConfig instanceof OverlayMetricReporterConfig overlayConfig
                ? Optional.of(overlayConfig)
                : Optional.empty();
    }

    static Optional<SubstrateMetricReporterConfig> substrateReporterConfig(Config config) {
        OciMetricReporterConfig reporterConfig = ociPublisher(config).prototype().reporterConfig();
        return reporterConfig instanceof SubstrateMetricReporterConfig substrateConfig
                ? Optional.of(substrateConfig)
                : Optional.empty();
    }
}
