/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for Auth certificates.
 */
@Prototype.Blueprint
@Prototype.Configured
interface AuthCertificateConfigBlueprint {

    /**
     * The local resource containing the certificate.
     *
     * @return the resource
     */
    @Option.Configured
    String certificate();

    /**
     * The local resource containing the private key.
     *
     * @return the resource
     */
    @Option.Configured
    Optional<String> privateKey();

    /**
     * The certificate's passphrase.
     *
     * @return the passphrase
     */
    @Option.Configured
    @Option.Default("")
    String passphrase();
}
