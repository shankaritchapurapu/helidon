/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.common;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.retrier.RetryOptions;

/**
 * Blueprint for OCI SDK retry configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
@Prototype.CustomMethods(ConfigSupport.RetryConfigurationSupport.class)
interface OciRetryConfigurationBlueprint {

    /**
     * OCI termination strategy for retry processing.
     *
     * @return optional termination strategy
     */
    @Option.Configured
    Optional<com.oracle.bmc.waiter.TerminationStrategy> terminationStrategy();

    /**
     * OCI delay strategy for retry processing.
     *
     * @return optional delay strategy
     */
    @Option.Configured
    Optional<com.oracle.bmc.waiter.DelayStrategy> delayStrategy();

    /**
     * OCI retry condition for retry processing.
     *
     * @return optional retry condition
     */
    @Option.Configured
    Optional<com.oracle.bmc.retrier.RetryCondition> retryCondition();

    /**
     * OCI retry options.
     *
     * @return optional retry options
     */
    @Option.Configured
    Optional<RetryOptions> retryOptions();
}
