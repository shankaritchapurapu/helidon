/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.builder.api.Prototype;
import io.helidon.common.Errors;

final class OciAutoHttpRuntimeDimensionConfigSupport
        implements Prototype.BuilderDecorator<OciAutoHttpRuntimeDimensionConfig.BuilderBase<?, ?>> {

    OciAutoHttpRuntimeDimensionConfigSupport() {
    }

    @Override
    public void decorate(OciAutoHttpRuntimeDimensionConfig.BuilderBase<?, ?> builder) {
        Errors.Collector errors = Errors.collector();
        requireNonBlank(errors, "property-name", builder.propertyName());
        requireNonBlank(errors, "dimension-name", builder.dimensionName());
        errors.collect().checkValid();
    }

    private static void requireNonBlank(Errors.Collector errors, String key, Optional<String> value) {
        if (value.isEmpty() || value.get().trim().isEmpty()) {
            errors.fatal("auto-http.runtime-dimension." + key + " must be configured.");
        }
    }
}
