/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Generated config backing for {@link SecretServiceConfigSourceBuilder}.
 */
@Prototype.Blueprint(decorator = SecretServiceConfigSourceConfigSupport.class)
@Prototype.Configured
@Prototype.CustomMethods(SecretServiceConfigSourceConfigSupport.class)
interface SecretServiceConfigSourceConfigBlueprint {
    /**
     * Config key prefix used by this source.
     *
     * @return configured key prefix
     */
    @Option.Configured
    @Option.Default(Ssv2Client.DEFAULT_PREFIX)
    String prefix();

    /**
     * Source-level cache TTL for tracked secret values.
     *
     * @return cache TTL
     */
    @Option.Configured
    @Option.Default("PT5M")
    Duration cacheTtl();

    /**
     * Background polling interval used to detect external secret changes.
     *
     * @return poll interval if explicitly configured
     */
    @Option.Configured
    Optional<Duration> pollInterval();

    /**
     * SSv2 client settings.
     *
     * @return SSv2 client settings
     */
    @Option.Configured
    @Option.DefaultMethod("create")
    Ssv2ClientConfig client();
}
