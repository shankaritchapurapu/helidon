/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;

/**
 * Blueprint for OCI SDK circuit breaker configuration.
 */
@Prototype.Configured
@Prototype.Blueprint
@Prototype.CustomMethods(ConfigSupport.CircuitBreakerConfigurationSupport.class)
interface OciCircuitBreakerConfigurationBlueprint extends Prototype.Factory<CircuitBreakerConfiguration> {

    /**
     * Failure-rate threshold for opening the circuit breaker.
     *
     * @return optional failure-rate threshold
     */
    @Option.Configured
    Optional<Integer> failureRateThreshold();

    /**
     * Slow-call-rate threshold for opening the circuit breaker.
     *
     * @return optional slow-call-rate threshold
     */
    @Option.Configured
    Optional<Integer> slowCallRateThreshold();

    /**
     * Time the circuit breaker remains open before transitioning to half-open.
     *
     * @return optional open-state wait duration
     */
    @Option.Configured
    Optional<Duration> waitDurationInOpenState();

    /**
     * Number of permitted calls while the circuit breaker is half-open.
     *
     * @return optional permitted call count
     */
    @Option.Configured
    Optional<Integer> permittedNumberOfCallsInHalfOpenState();

    /**
     * Minimum number of calls before calculating circuit-breaker state.
     *
     * @return optional minimum call count
     */
    @Option.Configured
    Optional<Integer> minimumNumberOfCalls();

    /**
     * Sliding window size used to evaluate failures and slow calls.
     *
     * @return optional sliding window size
     */
    @Option.Configured
    Optional<Integer> slidingWindowSize();

    /**
     * Threshold for treating a call as slow.
     *
     * @return optional slow-call duration threshold
     */
    @Option.Configured
    Optional<Duration> slowCallDurationThreshold();

    /**
     * Whether circuit-breaker stack traces should be writable.
     *
     * @return optional writable-stack-trace flag
     */
    @Option.Configured
    Optional<Boolean> writableStackTraceEnabled();
}
