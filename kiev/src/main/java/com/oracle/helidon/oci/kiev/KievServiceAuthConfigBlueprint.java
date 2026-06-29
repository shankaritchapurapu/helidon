/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalAuthConfig;

/**
 * Authentication settings for Kiev as a service.
 */
@Prototype.Blueprint(decorator = KievServiceAuthConfigSupport.class)
@Prototype.Configured
interface KievServiceAuthConfigBlueprint {

    /**
     * Authentication type.
     *
     * @return authentication type
     */
    @Option.Configured
    @Option.Default("INSTANCE")
    KievAuthType type();

    /**
     * Identity authentication endpoint.
     *
     * @return optional auth endpoint
     */
    @Option.Configured
    Optional<String> authEndpoint();

    /**
     * TLS settings for the auth mode.
     *
     * @return optional TLS configuration
     */
    @Option.Configured
    Optional<KievServiceTlsConfig> tls();

    /**
     * Service-principal settings for S2S auth.
     *
     * @return optional service-principal configuration
     */
    @Option.Configured
    Optional<ServicePrincipalAuthConfig> servicePrincipal();
}
