/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.URI;
import java.util.Optional;

import io.helidon.config.NamedService;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.pic.telemetry.commons.metrics.MetricReporter;

/**
 * Configuration settings for creating the OCI metrics-lib {@link MetricReporter} common to all specific reporter types.
 */
public interface OciMetricReporterConfig extends NamedService {

    /**
     * OCI project used for emitted telemetry.
     *
     * @return optional project name
     */
    Optional<String> project();

    /**
     * OCI fleet used for emitted telemetry.
     *
     * @return optional fleet name
     */
    Optional<String> fleet();

    /**
     * Optional OCI SDK client configuration.
     *
     * @return optional client configuration
     */
    Optional<ClientConfiguration> client();

    /**
     * Optional metrics backend endpoint override.
     *
     * @return optional endpoint
     */
    Optional<URI> endpoint();

    /**
     * Host name override for emitted dimensions.
     *
     * @return optional host name
     */
    Optional<String> hostname();

    /**
     * Availability domain override for emitted dimensions.
     *
     * @return optional availability domain
     */
    Optional<String> availabilityDomain();

    /**
     * Fault domain override for emitted dimensions.
     *
     * @return optional fault domain
     */
    Optional<String> faultDomain();

    /**
     * Region override for client setup and dimensions.
     *
     * @return optional region
     */
    Optional<String> region();

    /**
     * Create a reporter from this configuration.
     *
     * @return reporter
     */
    MetricReporter createReporter();

}
