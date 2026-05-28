/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.pic.bling.clients.BlingPublisherClient;

/**
 * Data plane metering configuration mapped from {@code oci.metering}.
 * <p>
 * This blueprint adapts Helidon config to the native emitter-dp configuration object. Keep native-backed settings
 * optional here unless Helidon must require them, and let defaults from the native configuration object apply.
 */
@Prototype.Blueprint(decorator = ConfigSupport.MeteringConfigSupport.class)
@Prototype.Configured("oci.metering")
@Prototype.CustomMethods(ConfigSupport.MeteringConfigSupport.class)
interface MeteringConfigBlueprint {
    /**
     * Whether to start the native reporting agent.
     *
     * @return {@code true} to start the agent
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

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
    String meteringDir();

    /**
     * Metering client ID.
     *
     * @return client ID
     */
    @Option.Configured
    String clientId();

    /**
     * Service name reported to Bling.
     *
     * @return service name
     */
    @Option.Configured
    String service();

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
     * Interval for reporting archived usage to Bling.
     *
     * @return report interval
     */
    @Option.Configured
    Optional<Duration> reportInterval();

    /**
     * OCI region used by services that need an explicit region. Defaults to the region from the current environment.
     *
     * @return public region name
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Host name to report in generated metering payloads.
     *
     * @return host name
     */
    @Option.Configured
    Optional<String> hostName();

    /**
     * Bling publisher client to use for reporting archived usage.
     *
     * @return publisher client
     */
    @Option.Configured
    BlingPublisherClient blingPublisherClient();

}
