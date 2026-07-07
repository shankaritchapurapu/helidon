/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.function.BiFunction;

import io.helidon.builder.api.Prototype;
import io.helidon.metrics.api.Meter;

final class OciMetricsPublisherConfigSupport
        implements Prototype.BuilderDecorator<OciMetricsPublisherConfig.BuilderBase<?, ?>> {
    static final String DEFAULT_METRICS_SCOPE_NAME = "service";

    OciMetricsPublisherConfigSupport() {
    }

    static OciMetricReporterConfig defaultReporterConfig() {
        return OverlayMetricReporterConfig.create();
    }

    @Override
    public void decorate(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        applyReporter(builder);
        applyMetricFilter(builder);
    }

    private static void applyMetricFilter(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        BiFunction<String, Meter, Boolean> filter = builder.filter().orElse(null);
        // Refresh a copied or previously generated default filter so it reflects the builder's current settings.
        if (filter == null || filter instanceof ReporterMetricFilter) {
            builder.filter(ReporterMetricFilter.create(builder));
        }
    }

    private static void applyReporter(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        OciMetricReporterConfig reporterConfig = reporterConfig(builder);
        builder.reporterConfig(reporterConfig);
        if (builder.reporter().isPresent()) {
            return;
        }

        builder.reporter(reporterConfig.createReporter());
    }

    private static OciMetricReporterConfig reporterConfig(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        if (builder.reporterConfig().isPresent()) {
            return builder.reporterConfig().orElseThrow();
        }

        return defaultReporterConfig();
    }

}
