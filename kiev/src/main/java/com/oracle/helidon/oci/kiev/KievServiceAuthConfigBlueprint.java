/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Authentication settings for Kiev as a service.
 */
@Prototype.Blueprint
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
     * S2S-specific settings for the auth mode.
     *
     * @return optional S2S configuration
     */
    @Option.Configured
    Optional<KievServiceS2sConfig> s2s();
}
