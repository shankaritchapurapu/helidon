/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import io.helidon.metrics.api.Meter;

import com.oracle.pic.telemetry.commons.metrics.model.MetricName;

/**
 * Utility methods for OCI telemetry metric names.
 */
final class OciMetricNames {

    private static final String SCOPE_DIMENSION = "scope";

    private OciMetricNames() {
    }

    /**
     * Creates an OCI {@link com.oracle.pic.telemetry.commons.metrics.model.MetricName} from a Helidon
     * {@link io.helidon.metrics.api.Meter}.
     *
     * @param meter Helidon meter
     * @return OCI {@code MetricName} corresponding to the Helidon meter
     */
    static MetricName create(Meter meter) {
        return create(meter, meter.id().name());
    }

    static MetricName create(Meter meter, String name) {
        Map<String, String> dimensions = new LinkedHashMap<>(meter.id().tagsMap());
        meter.scope().ifPresent(scope -> dimensions.put(SCOPE_DIMENSION, scope));
        return MetricName.of(name, dimensions);
    }
}
