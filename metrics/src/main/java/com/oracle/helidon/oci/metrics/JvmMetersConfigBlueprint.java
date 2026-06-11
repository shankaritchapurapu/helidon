/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for built-in JVM meters.
 */
@Prototype.Blueprint
@Prototype.Configured
interface JvmMetersConfigBlueprint {

    /**
     * Whether memory-usage gauges are enabled.
     *
     * @return memory-usage enabled flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean memoryUsageEnabled();

    /**
     * Whether thread-state gauges are enabled.
     *
     * @return thread-state enabled flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean threadStateEnabled();

    /**
     * Whether file-descriptor gauges are enabled.
     *
     * @return file-descriptor enabled flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean fileDescriptorEnabled();

    /**
     * Whether garbage-collection gauges are enabled.
     *
     * @return garbage-collection enabled flag
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean gcEnabled();
}
