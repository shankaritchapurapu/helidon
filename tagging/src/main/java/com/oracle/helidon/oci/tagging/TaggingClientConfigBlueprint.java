/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tagging;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for the OCI tagging integration.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.tagging")
interface TaggingClientConfigBlueprint {
    /**
     * Whether the OCI tagging client should emit metrics.
     *
     * @return {@code true} if metrics emission is enabled
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean emitMetrics();
}
