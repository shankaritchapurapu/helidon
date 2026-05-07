/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Data plane metering configuration mapped from {@code oci.metering}.
 * <p>
 * This blueprint adapts Helidon config to the native emitter-dp configuration object. Keep native-backed settings
 * optional here unless Helidon must require them, and let defaults from the native configuration object apply.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.metering")
interface MeteringConfigBlueprint {
    /**
     * Bling ingest endpoint.
     *
     * @return endpoint
     */
    @Option.Configured
    String endpoint();

    /**
     * Period used by the emitter for metering work.
     *
     * @return metering period
     */
    @Option.Configured
    Optional<Duration> meteringPeriod();

    /**
     * Duration for retaining archived metering data.
     *
     * @return archiving duration
     */
    @Option.Configured
    Optional<Duration> archivingDuration();

    /**
     * Local directory used by the emitter.
     *
     * @return metering directory
     */
    @Option.Configured
    Optional<String> meteringDir();

    /**
     * Metering client ID.
     *
     * @return client ID
     */
    @Option.Configured
    Optional<String> clientId();

    /**
     * Service name reported to Bling.
     *
     * @return service name
     */
    @Option.Configured
    Optional<String> service();

    /**
     * Whether Object Storage reporting is enabled.
     *
     * @return {@code true} when Object Storage reporting is enabled
     */
    @Option.Configured
    Optional<Boolean> osEnabled();

    /**
     * Whether the deployment is Kubernetes-based.
     *
     * @return {@code true} for Kubernetes deployments
     */
    @Option.Configured
    Optional<Boolean> k8sBasedDeployment();

    /**
     * Object Storage bucket name used by the emitter.
     *
     * @return bucket name
     */
    @Option.Configured
    Optional<String> bucketName();

    /**
     * Object Storage namespace used by the emitter.
     *
     * @return namespace
     */
    @Option.Configured
    Optional<String> namespace();

    /**
     * Frequency for reporting archived usage to Bling.
     *
     * @return report frequency
     */
    @Option.Configured
    Optional<Integer> reportToBlingFrequency();

    /**
     * Host name to report in generated metering payloads.
     *
     * @return host name
     */
    @Option.Configured
    Optional<String> hostName();
}
