/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.Api;

/**
 * Configuration for a reusable dynamic SSL context provider.
 */
@Prototype.Blueprint(decorator = DynamicSslProviderConfigSupport.class)
@Prototype.Configured
@Api.Internal
interface DynamicSslProviderConfigBlueprint {

    /**
     * Provider name used for service registry lookup and references.
     *
     * @return provider name
     */
    @Option.Configured
    String name();

    /**
     * Leaf certificate path.
     *
     * @return optional leaf certificate path
     */
    @Option.Configured
    Optional<String> leafCertPath();

    /**
     * Leaf certificate key path.
     *
     * @return optional leaf certificate key path
     */
    @Option.Configured
    Optional<String> leafCertKeyPath();

    /**
     * Leaf certificate key passphrase.
     *
     * @return optional leaf certificate key passphrase
     */
    @Option.Configured
    Optional<String> leafCertKeyPassphrase();

    /**
     * Intermediate certificate path.
     *
     * @return optional intermediate certificate path
     */
    @Option.Configured
    Optional<String> intermediateCertPath();

    /**
     * Root certificate path.
     *
     * @return root certificate path
     */
    @Option.Configured
    String rootCertPath();

    /**
     * Alias for {@link #rootCertPath()} used by Kiev TLS configuration.
     *
     * @return optional root certificate PEM path
     */
    @Option.Configured
    Optional<String> rootCertPemPath();

    /**
     * Certificate reload duration.
     *
     * @return optional reload duration
     */
    @Option.Configured
    Optional<Duration> duration();

    /**
     * Alias for {@link #duration()} used by Kiev TLS configuration.
     *
     * @return optional certificate reload duration
     */
    @Option.Configured
    Optional<Duration> certReloadDuration();

    /**
     * SSL algorithm.
     *
     * @return optional SSL algorithm
     */
    @Option.Configured
    Optional<String> sslAlgorithm();

    /**
     * Alias for {@link #sslAlgorithm()} used by Kiev TLS configuration.
     *
     * @return optional certificate SSL algorithm
     */
    @Option.Configured
    Optional<String> certSslAlgorithm();
}
