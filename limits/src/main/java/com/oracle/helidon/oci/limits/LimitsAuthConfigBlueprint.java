/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalAuthConfig;

/**
 * Authentication settings for the Limits data plane client.
 * <p>
 * Limits currently supports only {@code service-principal}. The configuration is still shaped as an
 * authentication-method selector with provider-specific sections so additional authentication providers can follow
 * the same pattern.
 */
@Prototype.Blueprint
@Prototype.Configured
interface LimitsAuthConfigBlueprint {
    /**
     * Authentication method name.
     *
     * @return authentication method
     */
    @Option.Configured
    @Option.Default("service-principal")
    String authenticationMethod();

    /**
     * Service-principal-specific settings.
     *
     * @return optional service-principal configuration
     */
    @Option.Configured
    Optional<ServicePrincipalAuthConfig> servicePrincipal();

}
