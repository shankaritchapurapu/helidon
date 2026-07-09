/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for automatic HTTP runtime dimensions.
 */
@Prototype.Blueprint(decorator = OciAutoHttpRuntimeDimensionConfigSupport.class)
@Prototype.Configured(value = "runtime-dimension", root = false)
interface OciAutoHttpRuntimeDimensionConfigBlueprint {

    /**
     * Request context property name to read for the runtime dimension value.
     *
     * @return property name
     */
    @Option.Configured
    String propertyName();

    /**
     * Metric dimension name to emit.
     *
     * @return dimension name
     */
    @Option.Configured
    String dimensionName();

    /**
     * Optional default dimension value when the request context does not provide a value.
     *
     * @return optional default dimension value
     */
    @Option.Configured
    Optional<String> defaultDimension();

}
