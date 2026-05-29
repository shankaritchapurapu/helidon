/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * TLS settings for Kiev service authentication.
 */
@Prototype.Blueprint(decorator = KievServiceTlsConfigSupport.class)
@Prototype.Configured
interface KievServiceTlsConfigBlueprint {

    /**
     * Root certificate PEM path.
     *
     * @return optional root certificate path
     */
    @Option.Configured
    Optional<String> rootCertPemPath();

    /**
     * Alias for {@link #rootCertPemPath()} using the common config name.
     *
     * @return optional root certificate path alias
     */
    @Option.Configured
    Optional<String> rootCertPath();

    /**
     * Name of a reusable dynamic SSL context provider.
     *
     * @return optional provider name
     */
    @Option.Configured
    Optional<String> dynamicSslContextProviderName();

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
