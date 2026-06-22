/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for a service-principal authentication certificate.
 */
@Prototype.Blueprint
@Prototype.Configured
interface ServicePrincipalCertificateConfigBlueprint {
    /**
     * Certificate resource location.
     * <p>
     * The value may point to a filesystem path, classpath resource, or any resource format supported by
     * {@link io.helidon.common.configurable.Resource}.
     *
     * @return certificate resource location
     */
    @Option.Configured
    String certificate();

    /**
     * Private key resource location.
     * <p>
     * The leaf certificate must provide a private key. Intermediate certificate entries may omit it.
     *
     * @return private key resource location
     */
    @Option.Configured
    Optional<String> privateKey();

    /**
     * Private key passphrase.
     *
     * @return private key passphrase, or an empty string when not configured
     */
    @Option.Configured
    @Option.Confidential
    @Option.Default("")
    String passphrase();
}
