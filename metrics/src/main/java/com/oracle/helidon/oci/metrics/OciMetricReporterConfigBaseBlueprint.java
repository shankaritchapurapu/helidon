/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.URI;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.helidon.oci.sdk.common.ConfigSupport;

/**
 * Common configured OCI metrics reporter settings.
 */
@Prototype.Blueprint(isPublic = false,
                     builderPublic = false,
                     createEmptyPublic = false,
                     createFromConfigPublic = false)
@Prototype.Configured(root = false)
@Prototype.CustomMethods(ConfigSupport.ClientConfigurationOptionSupport.class)
interface OciMetricReporterConfigBaseBlueprint {

    /**
     * OCI project used for emitted telemetry.
     *
     * @return optional project name
     */
    @Option.Configured
    Optional<String> project();

    /**
     * OCI fleet used for emitted telemetry.
     *
     * @return optional fleet name
     */
    @Option.Configured
    Optional<String> fleet();

    /**
     * Optional OCI SDK client configuration.
     *
     * @return optional client configuration
     */
    @Option.Configured
    Optional<ClientConfiguration> client();

    /**
     * Optional metrics backend endpoint override.
     *
     * @return optional endpoint
     */
    @Option.Configured
    Optional<URI> endpoint();

    /**
     * Host name override for emitted dimensions.
     *
     * @return optional host name
     */
    @Option.Configured
    Optional<String> hostname();

    /**
     * Host name override alias for emitted dimensions.
     *
     * @return optional host name
     */
    @Option.Configured("host-name")
    Optional<String> hostName();

    /**
     * Availability domain override for emitted dimensions.
     *
     * @return optional availability domain
     */
    @Option.Configured
    Optional<String> availabilityDomain();

    /**
     * Fault domain override for emitted dimensions.
     *
     * @return optional fault domain
     */
    @Option.Configured
    Optional<String> faultDomain();

    /**
     * Region override for client setup and dimensions.
     *
     * @return optional region
     */
    @Option.Configured
    Optional<String> region();
}
