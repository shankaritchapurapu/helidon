/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * SSv2 client configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
interface Ssv2ClientConfigBlueprint {
    /**
     * Whether the SSv2 client is enabled.
     *
     * @return {@code true} when enabled
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * SSv2 endpoint template.
     *
     * @return endpoint template
     */
    @Option.Configured
    @Option.Default(Ssv2Client.DEFAULT_ENDPOINT)
    String endpoint();

    /**
     * TLS configuration.
     *
     * @return TLS configuration
     */
    @Option.Configured
    @Option.DefaultMethod("create")
    Ssv2TlsConfig tlsConfig();

    /**
     * Retry configuration.
     *
     * @return retry configuration
     */
    @Option.Configured
    @Option.DefaultMethod("create")
    Ssv2RetryConfig retryConfig();
}
