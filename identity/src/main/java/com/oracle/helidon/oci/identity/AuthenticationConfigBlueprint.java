/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for Authentication.
 */
@Prototype.Blueprint
@Prototype.Configured
interface AuthenticationConfigBlueprint {

    /**
     * Whether the authentication is enabled.
     *
     * @return {@code true} if enabled; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * The authentication service URI. If not specified, it will be based on
     * the {@link #region()}.
     *
     * @return authentication URI
     */
    @Option.Configured
    Optional<URI> serviceUri();

    /**
     * Global business unit.
     *
     * @return the BU
     */
    @Option.Configured
    String globalBusinessUnit();

    /**
     * The team name.
     *
     * @return team name
     */
    @Option.Configured
    String teamName();

    /**
     * The application name.
     *
     * @return application name
     */
    @Option.Configured
    String applicationName();

    /**
     * The region in which to authenticate.
     *
     * @return the region
     */
    @Option.Configured
    String region();

    /**
     * Use instance principal certificates to access Auth service. If set to
     * {@code true} then {@link #instancePrincipalUri()} can be used to override
     * the default URI.
     *
     * @return whether to use instance principal or not
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean useInstancePrincipal();

    /**
     * URI from where to retrieve IMDS certificates. Override default URI for
     * IMDS. For testing purpose, an SSH tunnel can be created to retrieve
     * these certificates.
     *
     * @return URI for IMDS certificates.
     */
    @Option.Configured
    Optional<URI> instancePrincipalUri();

    /**
     * List of certificates to use, intermediate and leaf ones. Ignored if
     * {@link #useInstancePrincipal()} is set to {@code true}.
     *
     * @return the certificate
     */
    @Option.Configured
    List<AuthCertificateConfig> certificates();

    /**
     * Path to root certificate.
     *
     * @return root certificate
     */
    @Option.Configured
    Optional<String> rootCertPath();

    /**
     * Name of a metrics library to use. For example, "telemetry" or
     * "commons". Enables metrics when provided.
     *
     * @return name of a metrics library
     */
    @Option.Configured
    Optional<String> metricsLib();

    /**
     * Use a hard-coded key supplier instead of requesting keys
     * from the Identity service. This can be used for testing.
     *
     * @return whether to use a hard-coded key supplier or not
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean hardCodedKeySupplier();
}
