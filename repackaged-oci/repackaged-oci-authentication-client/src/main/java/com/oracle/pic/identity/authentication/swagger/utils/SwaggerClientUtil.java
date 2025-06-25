/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.pic.identity.authentication.swagger.utils;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.circuitbreaker.CircuitBreakerFactory;
import com.oracle.bmc.circuitbreaker.OciCircuitBreaker;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import java.time.Duration;
import java.util.Random;

public abstract class SwaggerClientUtil {
    public static final Random RANDOM = new Random();

    public SwaggerClientUtil() {
        super();
    }

    public static RetryConfiguration getInstanceOfRetryConfiguration(int retries) {
        return RetryConfiguration.builder().terminationStrategy(new MaxAttemptsTerminationStrategy(retries)).build();
    }

    public static ClientConfiguration getInstanceOfClientConfiguration(int retries) {
        return ClientConfiguration.builder().retryConfiguration(getInstanceOfRetryConfiguration(retries)).circuitBreaker(getInstanceOfCircuitBreaker()).build();
    }

    public static OciCircuitBreaker getInstanceOfCircuitBreaker() {
        return CircuitBreakerFactory.build(CircuitBreakerConfiguration.builder().waitDurationInOpenState(Duration.ofSeconds((long)(30 + RANDOM.nextInt(10)))).build());
    }
}
