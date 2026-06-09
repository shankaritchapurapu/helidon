/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.metrics.api.MetricsPublisherConfig;
import io.helidon.metrics.spi.MetricsPublisherProvider;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.helidon.oci.sdk.common.ConfigSupport;

/**
 * OCI metrics publisher configuration.
 */
@Prototype.Blueprint(decorator = OciMetricsPublisherConfigSupport.class)
@Prototype.Configured(value = OciMetricsPublisher.TYPE, root = false)
@Prototype.CustomMethods(ConfigSupport.ClientConfigurationOptionSupport.class)
@Prototype.Provides(MetricsPublisherProvider.class)
interface OciMetricsPublisherConfigBlueprint extends MetricsPublisherConfig, Prototype.Factory<OciMetricsPublisher> {

    /**
     * Whether OCI metrics publishing is enabled.
     *
     * @return enabled flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

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
     * Optional monitoring service endpoint override.
     *
     * @return optional endpoint
     */
    @Option.Configured
    Optional<URI> endpoint();

    /**
     * Optional prebuilt monitoring client.
     *
     * @return optional monitoring client
     */
    Optional<Monitoring> monitoring();

    /**
     * Default dimensions for OCI `com.oracle.pic.telemetry.commons.metrics.Metrics.init`.
     *
     * @return default dimensions
     */
    @Option.Configured
    Map<String, String> defaultDimensions();

    /**
     * Whether to use the OCI metadata service when preparing telemetry.
     *
     * @return optional metadata-service flag
     */
    @Option.Configured
    Optional<Boolean> useMetadataService();

    /**
     * Host name override for emitted dimensions.
     *
     * @return optional host name
     */
    @Option.Configured
    Optional<String> hostname();

    /**
     * Alias for {@link #hostname()} using the common config name.
     *
     * @return optional host name alias
     */
    @Option.Configured
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
     * Region override for monitoring client setup and dimensions.
     *
     * @return optional region
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Whether OCI metric keys should be overridden.
     *
     * @return optional override flag
     */
    @Option.Configured
    Optional<Boolean> overrideMetricKeys();

    /**
     * Additional headers to send with OCI monitoring requests.
     *
     * @return request headers
     */
    @Option.Configured
    Map<String, String> requestHeaders();

    /**
     * Whether gauges should be sampled and published on a schedule.
     *
     * @return gauge sampling flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean sampleGauges();

    /**
     * Interval between scheduled gauge samples.
     *
     * @return gauge sample interval
     */
    @Option.Configured
    @Option.Default("PT1M")
    Duration gaugeSampleInterval();

    /**
     * Units used when reporting time-valued metrics.
     *
     * @return optional reporting time unit
     */
    @Option.Configured
    Optional<TimeUnit> reportingTimeUnit();

    /**
     * Configuration for built-in JVM meters.
     *
     * @return optional JVM meter configuration
     */
    @Option.Configured
    Optional<JvmMetersConfig> jvmMeters();
}
