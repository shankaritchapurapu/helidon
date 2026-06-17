/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.metrics.api.Meter;
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
     * Unit to report durations as.
     *
     * @return duration unit
     */
    @Option.Configured
    @Option.DefaultCode("java.util.concurrent.TimeUnit.MILLISECONDS")
    TimeUnit durationUnit();

    /**
     * Metrics scope name prefix for built-in JVM meters.
     *
     * @return metrics scope name
     */
    @Option.Configured
    @Option.Default(OciMetricsPublisherConfigSupport.DEFAULT_METRICS_SCOPE_NAME)
    String metricsScopeName();

    /**
     * Metric names to exclude from reporting.
     *
     * @return excluded metric names
     */
    @Option.Configured
    Set<String> excludes();

    /**
     * Metric names to include in reporting.
     *
     * @return included metric names
     */
    @Option.Configured
    Set<String> includes();

    /**
     * Whether include and exclude values are treated as regular expressions.
     *
     * @return regex filter flag
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean useRegexFilters();

    /**
     * Whether include and exclude values are treated as substrings.
     *
     * @return substring matching flag
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean useSubstringMatching();

    /**
     * Metric attributes to exclude from reporting.
     *
     * @return excluded metric attributes
     */
    @Option.Configured
    Set<String> excludesAttributes();

    /**
     * Metric attributes to include in reporting.
     *
     * @return included metric attributes
     */
    @Option.Configured
    @Option.DefaultCode("""
            new java.util.LinkedHashSet<>(java.util.List.of("value",
                                                            "max", "mean", "min", "stddev",
                                                            "p50", "p75", "p95", "p98", "p99", "p999",
                                                            "count", "m1_rate", "m5_rate",
                                                            "m15_rate", "mean_rate"))
            """)
    Set<String> includesAttributes();

    /**
     * Programmatic filter for metric updates.
     *
     * @return metric update filter
     */
    @Option.Redundant(equality = false, stringValue = true)
    BiFunction<String, Meter, Boolean> filter();

    /**
     * Whether detailed automatic HTTP timing metrics should be emitted.
     *
     * @return detailed automatic HTTP timing flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enableDetailedTimingAutoMetrics();

    /**
     * Optional resource package prefix for automatic HTTP metrics.
     *
     * @return optional resource package prefix
     */
    @Option.Configured
    Optional<String> resourcePackagePrefix();

    /**
     * Interval between scheduled gauge samples.
     *
     * @return gauge sample interval
     */
    @Option.Configured
    @Option.Default("PT1M")
    Duration gaugeSampleInterval();

}
