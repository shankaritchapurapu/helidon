/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.nio.file.Path;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for {@code oci-env.dynamic-core-regions}.
 */
@Prototype.Blueprint
@Prototype.Configured
interface OciEnvDynamicCoreRegionsBlueprint {

    /**
     * Whether dynamic core-regions import is enabled.
     *
     * @return {@code true} when dynamic core-regions import is enabled
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * Primary dynamic core-regions import path.
     *
     * @return configured import path
     */
    @Option.Configured
    @Option.Default(DynamicCoreRegions.DEFAULT_IMPORT_PATH)
    Path importPath();

    /**
     * Dynamic core-regions override import path.
     *
     * @return configured override import path
     */
    @Option.Configured
    @Option.Default(DynamicCoreRegions.DEFAULT_IMPORT_OVERRIDE_PATH)
    Path importOverridePath();

    /**
     * Region name used to validate the imported dynamic metadata.
     *
     * @return validation region name
     */
    @Option.Configured
    @Option.Default(DynamicCoreRegions.DEFAULT_VALIDATION_REGION)
    String validationRegion();

    /**
     * Whether to validate the imported dynamic metadata after import.
     *
     * @return {@code true} to validate the imported metadata
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean validateImport();
}
