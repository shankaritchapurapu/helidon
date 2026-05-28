/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.retrier.RetryOptions;

/**
 * Blueprint for OCI SDK retry options.
 */
@Prototype.Blueprint
@Prototype.Configured
@Prototype.CustomMethods(ConfigSupport.RetryOptionsSupport.class)
interface OciRetryOptionsBlueprint extends Prototype.Factory<RetryOptions> {

    /**
     * Mark-read limit used for retryable request bodies.
     *
     * @return optional mark-read limit
     */
    @Option.Configured("mark-read-limit")
    Optional<Integer> markReadLimit();
}
