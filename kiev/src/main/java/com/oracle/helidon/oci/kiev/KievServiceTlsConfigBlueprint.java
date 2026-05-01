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
@Prototype.Blueprint
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
