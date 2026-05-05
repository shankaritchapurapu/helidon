/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.RetryOnOpenCircuitBreakerDefaultRetryCondition;
import com.oracle.bmc.waiter.ExponentialBackoffDelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.WaiterConfiguration;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class OciMetricsPublisherConfigMappingTest {

    private static final Config CONFIG = Config.just(
            ConfigSources.create("""
                                         metrics:
                                           publishers:
                                             - type: oci
                                               enabled: true
                                               project: test-project
                                               fleet: test-fleet
                                               client-configuration:
                                                 connection-timeout: PT7S
                                                 read-timeout: PT11S
                                                 max-async-threads: 13
                                                 retry-configuration:
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
                                                 circuit-breaker-configuration:
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
    void mapsPublisherYamlToTopLevelClientConfiguration() {
        OciMetricsPublisherConfig publisherConfig = publisherConfig();
        ClientConfiguration clientConfiguration = publisherConfig.clientConfiguration().orElseThrow();

        assertThat(publisherConfig.project().orElseThrow(), is("test-project"));
        assertThat(publisherConfig.fleet().orElseThrow(), is("test-fleet"));
        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is(7000));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is(11000));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(13));
    }

    @Test
    void mapsPublisherYamlToRetryConfiguration() {
        ClientConfiguration clientConfiguration = publisherConfig().clientConfiguration().orElseThrow();
        RetryConfiguration retryConfiguration = clientConfiguration.getRetryConfiguration();

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
    void mapsPublisherYamlToCircuitBreakerConfiguration() {
        ClientConfiguration clientConfiguration = publisherConfig().clientConfiguration().orElseThrow();
        CircuitBreakerConfiguration circuitBreakerConfiguration = clientConfiguration.getCircuitBreakerConfiguration();

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

    private static OciMetricsPublisherConfig publisherConfig() {
        return OciMetricsPublisherConfig.create(CONFIG.get("metrics")
                                                        .get("publishers")
                                                        .get("0"));
    }
}
