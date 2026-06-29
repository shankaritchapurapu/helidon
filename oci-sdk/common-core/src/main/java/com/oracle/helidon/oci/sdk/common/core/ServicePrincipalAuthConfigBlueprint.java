/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration of the {@code service-principal} authentication method.
 */
@Prototype.Blueprint
@Prototype.Configured
interface ServicePrincipalAuthConfigBlueprint {
    /**
     * Identity federation endpoint used by service-principal authentication.
     *
     * @return optional federation endpoint
     */
    @Option.Configured
    Optional<URI> federationEndpoint();

    /**
     * OCI tenant id used by service-principal authentication.
     *
     * @return optional tenant id
     */
    @Option.Configured
    Optional<String> tenantId();

    /**
     * IMDS base URI override used by service-principal authentication.
     *
     * @return optional IMDS base URI
     */
    @Option.Configured
    Optional<URI> imdsBaseUri();

    /**
     * Whether to use service-principal configuration provided by the deployment platform.
     * <p>
     * When enabled, certificate material is obtained from platform-provided S2S configuration such as IDMS, ODO, or
     * OMK. When disabled, {@link #certificates()} must provide the service-principal certificate chain, with the leaf
     * certificate first.
     *
     * @return whether to use platform-provided service-principal configuration
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean usePlatformProvided();

    /**
     * Explicit service-principal certificate chain.
     * <p>
     * The first entry is the leaf certificate and must include its private key. Remaining entries are treated as
     * intermediate certificates.
     *
     * @return configured service-principal certificate chain
     */
    @Option.Configured
    List<ServicePrincipalCertificateConfig> certificates();
}
