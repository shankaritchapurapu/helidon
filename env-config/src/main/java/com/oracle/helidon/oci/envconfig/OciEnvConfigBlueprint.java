/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for the {@code oci-env} meta-config source properties.
 */
@Prototype.Blueprint
@Prototype.Configured
interface OciEnvConfigBlueprint {

    /**
     * Prefix used for the published config keys.
     *
     * @return configured prefix
     */
    @Option.Configured
    @Option.Default(OciEnvConfigFactory.DEFAULT_PREFIX)
    String prefix();

    /**
     * Optional region, availability-domain, and fault-domain overrides.
     *
     * @return configured location overrides, when present
     */
    @Option.Configured
    Optional<OciEnvLocationOverride> locationOverride();

    /**
     * Whether to read the physical availability-domain file.
     *
     * @return {@code true} to use {@code /etc/physical-availability-domain}
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean usePhysicalAvailabilityDomain();

    /**
     * Whether to override the location to {@code Region.DEV}/{@code AvailabilityDomain.DEV_1}.
     *
     * @return {@code true} to use the DEV location override
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean locationOverrideDev();

    /**
     * Dynamic core-regions import settings.
     *
     * @return configured import settings, when present
     */
    @Option.Configured
    Optional<OciEnvDynamicCoreRegions> dynamicCoreRegions();
}
