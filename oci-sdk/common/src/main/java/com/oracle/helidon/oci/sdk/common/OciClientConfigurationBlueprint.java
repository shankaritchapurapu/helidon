/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.retrier.RetryConfiguration;

/**
 * Blueprint for OCI SDK client configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
@Prototype.CustomMethods(ConfigSupport.ClientConfigurationSupport.class)
interface OciClientConfigurationBlueprint extends Prototype.Factory<ClientConfiguration> {

    /**
     * Connection timeout for OCI SDK requests.
     *
     * @return optional connection timeout
     */
    @Option.Configured
    Optional<Duration> connectionTimeout();

    /**
     * Read timeout for OCI SDK requests.
     *
     * @return optional read timeout
     */
    @Option.Configured
    Optional<Duration> readTimeout();

    /**
     * Maximum number of asynchronous client threads.
     *
     * @return optional maximum async thread count
     */
    @Option.Configured
    Optional<Integer> maxAsyncThreads();

    /**
     * Whether upload buffering should be disabled.
     *
     * @return optional buffering flag
     */
    @Option.Configured
    Optional<Boolean> disableDataBufferingOnUpload();

    /**
     * Retry behavior for OCI SDK requests.
     *
     * @return optional retry
     */
    @Option.Configured
    Optional<RetryConfiguration> retry();

    /**
     * Circuit breaker behavior for OCI SDK requests.
     *
     * @return optional circuit breaker
     */
    @Option.Configured
    Optional<CircuitBreakerConfiguration> circuitBreaker();
}
