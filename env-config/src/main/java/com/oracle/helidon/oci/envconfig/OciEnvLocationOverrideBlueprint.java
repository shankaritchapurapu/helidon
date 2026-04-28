/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for {@code oci-env.location-override}.
 */
@Prototype.Blueprint
@Prototype.Configured
interface OciEnvLocationOverrideBlueprint {

    /**
     * Region override value.
     *
     * @return configured region override, when present
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Availability-domain override value.
     *
     * @return configured availability-domain override, when present
     */
    @Option.Configured
    Optional<String> availabilityDomain();

    /**
     * Fault-domain override value.
     *
     * @return configured fault-domain override, when present
     */
    @Option.Configured
    Optional<Integer> faultDomain();
}
