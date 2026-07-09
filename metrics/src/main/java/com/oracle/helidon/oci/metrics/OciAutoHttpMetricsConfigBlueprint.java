/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for automatic HTTP metrics.
 */
@Prototype.Blueprint(decorator = OciAutoHttpMetricsConfigSupport.class)
@Prototype.Configured(value = "auto-http", root = false)
interface OciAutoHttpMetricsConfigBlueprint {

    /**
     * Whether automatic HTTP metrics should be gathered and emitted.
     *
     * @return enabled flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * Whether automatic HTTP metrics should include user-agent client aggregation counters.
     *
     * @return user-agent metrics enabled flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean userAgentMetricsEnabled();

    /**
     * Maximum number of detailed user-agent metric series admitted before new client identities are aggregated as
     * {@code OTHER}. Stable client-family and {@code OTHER} series do not count against this limit.
     *
     * @return maximum detailed user-agent series
     */
    @Option.Configured
    @Option.DefaultInt(1000)
    int maxUserAgentSeries();

    /**
     * Optional service-core-compatible runtime dimension for automatic HTTP metrics.
     *
     * @return optional runtime dimension configuration
     */
    @Option.Configured("runtime-dimension")
    Optional<OciAutoHttpRuntimeDimensionConfig> runtimeDimension();

}
