/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.common;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint for OCI SDK retry options.
 */
@Prototype.Blueprint
@Prototype.Configured
interface OciRetryOptionsBlueprint {

    /**
     * Mark-read limit used for retryable request bodies.
     *
     * @return optional mark-read limit
     */
    @Option.Configured("mark-read-limit")
    Optional<Integer> markReadLimit();
}
