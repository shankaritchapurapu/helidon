/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.Region;
import com.oracle.helidon.oci.sdk.common.ConfigSupport;

/**
 * OCI limits configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
@Prototype.CustomMethods(ConfigSupport.ClientConfigurationOptionSupport.class)
interface LimitsConfigBlueprint {

    /**
     * OCI SDK client configuration.
     *
     * @return client configuration
     */
    @Option.Configured
    Optional<ClientConfiguration> client();

    /**
     * Region for the Limits service-to-service provider.
     *
     * @return optional region
     */
    @Option.Configured
    Optional<Region> region();

    /**
     * OCI authentication override for the Limits client.
     *
     * @return optional authentication override
     */
    @Option.Configured
    Optional<LimitsAuthConfig> auth();

    /**
     * Client target endpoint.
     *
     * @return endpoint
     */
    @Option.Configured
    Optional<String> endpoint();
}
