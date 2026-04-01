/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.time.Duration;
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
     * Root certificate PEM path.
     *
     * @return optional root certificate path
     */
    @Option.Configured
    Optional<String> rootCertPemPath();

    /**
     * Leaf certificate path for S2S auth.
     *
     * @return optional leaf certificate path
     */
    @Option.Configured
    Optional<String> leafCertPath();

    /**
     * Leaf private key path for S2S auth.
     *
     * @return optional private key path
     */
    @Option.Configured
    Optional<String> leafCertKeyPath();

    /**
     * Intermediate certificate path for S2S auth.
     *
     * @return optional intermediate certificate path
     */
    @Option.Configured
    Optional<String> intermediateCertPath();

    /**
     * Tenant id for S2S auth.
     *
     * @return optional tenant id
     */
    @Option.Configured
    Optional<String> tenantId();

    /**
     * Private key passphrase for S2S auth.
     *
     * @return optional passphrase
     */
    @Option.Configured
    Optional<String> keyPassphrase();

    /**
     * Certificate reload interval.
     *
     * @return optional reload duration
     */
    @Option.Configured
    Optional<Duration> certReloadDuration();

    /**
     * SSL algorithm for certificate handling.
     *
     * @return optional SSL algorithm
     */
    @Option.Configured
    Optional<String> certSslAlgorithm();
}
