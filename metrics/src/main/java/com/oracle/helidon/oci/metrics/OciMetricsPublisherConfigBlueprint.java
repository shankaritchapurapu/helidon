/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsPublisherConfig;
import io.helidon.metrics.spi.MetricsPublisherProvider;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;

/**
 * OCI metrics publisher configuration.
 */
@Prototype.Blueprint(decorator = OciMetricsPublisherConfigSupport.class)
@Prototype.Configured(value = OciMetricsPublisher.TYPE, root = false)
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
     * Reporter to pass to OCI `com.oracle.pic.telemetry.commons.metrics.Metrics.init`.
     *
     * @return metrics-lib reporter
     */
    @Option.Redundant
    MetricReporter reporter();

    /**
     * Configured reporter settings used when {@link #reporter()} is not supplied programmatically.
     *
     * @return reporter config
     */
    @Option.Configured("reporter")
    @Option.Provider(value = OciMetricReporterConfigProvider.class, discoverServices = false)
    OciMetricReporterConfig reporterConfig();

    /**
     * Default dimensions for OCI `com.oracle.pic.telemetry.commons.metrics.Metrics.init`.
     *
     * @return default dimensions
     */
    @Option.Configured
    Map<String, String> defaultDimensions();

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
     * Matching mode for include and exclude values.
     *
     * @return filter matching mode
     */
    @Option.Configured
    @Option.Default("EXACT")
    FilterMatchingMode filterMatchingMode();

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
            new java.util.LinkedHashSet<>(java.util.List.of("value"))
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
     * Interval between scheduled metric samples.
     *
     * @return sample interval
     */
    @Option.Configured
    @Option.Default("PT1S")
    Duration sampleInterval();

    /**
     * Bounded accumulator settings for event-driven meters.
     *
     * @return accumulator configuration
     */
    @Option.Configured("accumulators")
    @Option.DefaultCode("OciMetricsAccumulatorConfig.create()")
    OciMetricsAccumulatorConfig accumulators();

    /**
     * Automatic HTTP metrics async delivery settings.
     *
     * @return automatic HTTP metrics configuration
     */
    @Option.Configured("auto-http")
    @Option.DefaultCode("OciAutoHttpMetricsConfig.create()")
    OciAutoHttpMetricsConfig autoHttp();

}
