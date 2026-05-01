/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * S2S-specific authentication settings for Kiev service authentication.
 */
@Prototype.Blueprint
@Prototype.Configured
interface KievServiceS2sConfigBlueprint {

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
     * Private key passphrase for S2S auth.
     *
     * @return optional passphrase
     */
    @Option.Configured
    Optional<String> keyPassphrase();

    /**
     * Tenant id for S2S auth.
     *
     * @return optional tenant id
     */
    @Option.Configured
    Optional<String> tenantId();
}
