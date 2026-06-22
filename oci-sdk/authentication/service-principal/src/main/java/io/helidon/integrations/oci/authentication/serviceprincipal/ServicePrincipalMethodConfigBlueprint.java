/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration of the {@code service-principal} authentication method.
 */
@Prototype.Blueprint
@Prototype.Configured("helidon.oci.authentication.service-principal")
interface ServicePrincipalMethodConfigBlueprint {
    /**
     * Whether to obtain service-principal certificate material from instance principals and IMDS.
     * <p>
     * When disabled, {@link #certificates()} must provide the service-principal certificate chain, with the
     * leaf certificate first.
     *
     * @return whether to use instance-principal certificate material from IMDS
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean useInstancePrincipal();

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
