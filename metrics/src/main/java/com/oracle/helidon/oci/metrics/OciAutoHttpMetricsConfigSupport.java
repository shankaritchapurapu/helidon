/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.builder.api.Prototype;
import io.helidon.common.Errors;

final class OciAutoHttpMetricsConfigSupport
        implements Prototype.BuilderDecorator<OciAutoHttpMetricsConfig.BuilderBase<?, ?>> {

    OciAutoHttpMetricsConfigSupport() {
    }

    @Override
    public void decorate(OciAutoHttpMetricsConfig.BuilderBase<?, ?> builder) {
        Errors.Collector errors = Errors.collector();
        if (builder.maxUserAgentSeries() <= 0) {
            errors.fatal("metrics.publishers[].auto-http.max-user-agent-series must be greater than zero.");
        }
        errors.collect().checkValid();
    }
}
