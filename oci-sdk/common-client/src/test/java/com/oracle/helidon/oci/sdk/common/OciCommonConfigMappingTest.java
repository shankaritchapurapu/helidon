/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.RetryOnOpenCircuitBreakerDefaultRetryCondition;
import com.oracle.bmc.retrier.RetryOptions;
import com.oracle.bmc.waiter.ExponentialBackoffDelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.WaiterConfiguration;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class OciCommonConfigMappingTest {

    private static final Config CONFIG = Config.just(
            ConfigSources.create("""
                                         client:
                                           connection-timeout: PT7S
                                           read-timeout: PT11S
                                           max-async-threads: 13
                                           retry:
                                             termination-strategy:
                                               type: max-attempts
                                               max-attempts: 5
                                             delay-strategy:
                                               type: exponential
                                               max-delay: PT2S
                                             retry-condition:
                                               type: retry-on-open-circuit-breaker
                                             retry-options:
                                               mark-read-limit: 4096
                                           circuit-breaker:
                                             failure-rate-threshold: 77
                                             slow-call-rate-threshold: 66
                                             wait-duration-in-open-state: PT9S
                                             permitted-number-of-calls-in-half-open-state: 4
                                             minimum-number-of-calls: 3
                                             sliding-window-size: 22
                                             slow-call-duration-threshold: PT8S
                                             writable-stack-trace-enabled: false
                                         """, MediaTypes.APPLICATION_YAML));

    @Test
    void mapsClientConfiguration() {
        ClientConfiguration clientConfiguration = clientConfiguration();

        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is(7000));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is(11000));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(13));
    }

    @Test
    void mapsNestedRetryConfiguration() {
        RetryConfiguration retryConfiguration = clientConfiguration().getRetryConfiguration();

        assertThat(retryConfiguration, notNullValue());
        assertThat(retryConfiguration.getTerminationStrategy(), instanceOf(MaxAttemptsTerminationStrategy.class));
        MaxAttemptsTerminationStrategy terminationStrategy =
                (MaxAttemptsTerminationStrategy) retryConfiguration.getTerminationStrategy();
        assertThat(terminationStrategy.getMaxAttempts(), is(5));

        assertThat(retryConfiguration.getDelayStrategy(), instanceOf(ExponentialBackoffDelayStrategy.class));
        ExponentialBackoffDelayStrategy delayStrategy =
                (ExponentialBackoffDelayStrategy) retryConfiguration.getDelayStrategy();
        WaiterConfiguration.WaitContext waitContext = new WaiterConfiguration.WaitContext(0L);
        waitContext.incrementAttempts();
        assertThat(delayStrategy.nextDelay(waitContext), is(2000L));

        assertThat(retryConfiguration.getRetryCondition(),
                   instanceOf(RetryOnOpenCircuitBreakerDefaultRetryCondition.class));
        assertThat(retryConfiguration.getRetryOptions().getMarkReadLimit(), is(4096));
    }

    @Test
    void mapsNestedCircuitBreakerConfiguration() {
        CircuitBreakerConfiguration circuitBreakerConfiguration = clientConfiguration().getCircuitBreakerConfiguration();

        assertThat(circuitBreakerConfiguration, notNullValue());
        assertThat(circuitBreakerConfiguration.getFailureRateThreshold(), is(77));
        assertThat(circuitBreakerConfiguration.getSlowCallRateThreshold(), is(66));
        assertThat(circuitBreakerConfiguration.getWaitDurationInOpenState(), is(java.time.Duration.ofSeconds(9)));
        assertThat(circuitBreakerConfiguration.getPermittedNumberOfCallsInHalfOpenState(), is(4));
        assertThat(circuitBreakerConfiguration.getMinimumNumberOfCalls(), is(3));
        assertThat(circuitBreakerConfiguration.getSlidingWindowSize(), is(22));
        assertThat(circuitBreakerConfiguration.getSlowCallDurationThreshold(), is(java.time.Duration.ofSeconds(8)));
        assertThat(circuitBreakerConfiguration.isWritableStackTraceEnabled(), is(false));
    }

    @Test
    void generatedRetryOptionsFactoryBuildsOciRetryOptions() {
        RetryOptions retryOptions = OciRetryOptions.builder()
                .markReadLimit(1024)
                .build();

        assertThat(retryOptions.getMarkReadLimit(), is(1024));
    }

    @Test
    void generatedTerminationStrategyFactoryBuildsOciStrategy() {
        com.oracle.bmc.waiter.TerminationStrategy terminationStrategy =
                MaxAttemptsTerminationStrategyConfig.builder()
                        .maxAttempts(3)
                        .build();

        assertThat(terminationStrategy, instanceOf(MaxAttemptsTerminationStrategy.class));
        assertThat(((MaxAttemptsTerminationStrategy) terminationStrategy).getMaxAttempts(), is(3));
    }

    private static ClientConfiguration clientConfiguration() {
        return OciClientConfiguration.create(CONFIG.get("client")).build();
    }
}
