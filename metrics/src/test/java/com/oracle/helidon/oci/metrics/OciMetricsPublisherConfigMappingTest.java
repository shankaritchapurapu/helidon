/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class OciMetricsPublisherConfigMappingTest {

    private static final Config CONFIG = Config.just(
            ConfigSources.create("""
                                         metrics:
                                           publishers:
                                             - type: oci
                                               enabled: true
                                               project: test-project
                                               fleet: test-fleet
                                               region: us-ashburn-1
                                               enable-detailed-timing-auto-metrics: false
                                               resource-package-prefix: com.example.store
                                               duration-unit: seconds
                                               metrics-scope-name: custom-service
                                               includes:
                                                 - test.counter
                                                 - test.timer
                                               excludes:
                                                 - test.excluded
                                               use-regex-filters: true
                                               use-substring-matching: false
                                               includes-attributes:
                                                 - value
                                                 - count
                                               excludes-attributes:
                                                 - p999
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
    void mapsPublisherYamlToTopLevelClientConfiguration() {
        OciMetricsPublisherConfig publisherConfig = publisherConfig();
        ClientConfiguration clientConfiguration = publisherConfig.client().orElseThrow();

        assertThat(publisherConfig.project().orElseThrow(), is("test-project"));
        assertThat(publisherConfig.fleet().orElseThrow(), is("test-fleet"));
        assertThat(publisherConfig.region().orElseThrow(), is("us-ashburn-1"));
        assertThat(publisherConfig.resourcePackagePrefix().orElseThrow(), is("com.example.store"));
        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is(7000));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is(11000));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(13));
    }

    @Test
    void mapsPublisherYamlToRetryConfiguration() {
        ClientConfiguration clientConfiguration = publisherConfig().client().orElseThrow();
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
        ClientConfiguration clientConfiguration = publisherConfig().client().orElseThrow();
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

    @Test
    void mapsPublisherYamlToMetricFilteringConfig() {
        OciMetricsPublisherConfig publisherConfig = publisherConfig();

        assertThat(publisherConfig.durationUnit(), is(TimeUnit.SECONDS));
        assertThat(publisherConfig.metricsScopeName(), is("custom-service"));
        assertThat(publisherConfig.includes(), is(Set.of("test.counter", "test.timer")));
        assertThat(publisherConfig.excludes(), is(Set.of("test.excluded")));
        assertThat(publisherConfig.useRegexFilters(), is(true));
        assertThat(publisherConfig.useSubstringMatching(), is(false));
        assertThat(publisherConfig.includesAttributes(), is(Set.of("value", "count")));
        assertThat(publisherConfig.excludesAttributes(), is(Set.of("p999")));
        assertThat(publisherConfig.filter(), instanceOf(ReporterMetricFilter.class));
    }

    @Test
    void mapsPublisherYamlToSubstringMatchingConfig() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             use-substring-matching: true
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);

        assertThat(publisherConfig.useRegexFilters(), is(false));
        assertThat(publisherConfig.useSubstringMatching(), is(true));
    }

    @Test
    void failsWhenYamlEnablesRegexAndSubstringMatching() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             use-regex-filters: true
                                             use-substring-matching: true
                                             """, MediaTypes.APPLICATION_YAML));

        ConfigException exception = assertThrows(ConfigException.class, () -> OciMetricsPublisherConfig.create(config));

        assertThat(exception.getMessage(), is("OCI metrics publisher filter configuration is ambiguous: "
                                                      + "do not enable both use-regex-filters and use-substring-matching; "
                                                      + "choose either regex or substring matching."));
    }

    @Test
    void doesNotMapFilterFromYaml() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             includes:
                                               - test.counter
                                             filter: ignored
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);

        assertThat(publisherConfig.filter(), instanceOf(ReporterMetricFilter.class));
        assertThat(publisherConfig.filter().apply("test.counter", null), is(true));
        assertThat(publisherConfig.filter().apply("test.timer", null), is(false));
    }

    @Test
    void mapsDetailedTimingAutoMetricsFlag() {
        assertThat(publisherConfig().enableDetailedTimingAutoMetrics(), is(false));
    }

    @Test
    void mapsHostNameAlias() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             host-name: test-host
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);

        assertThat(publisherConfig.hostname().orElseThrow(), is("test-host"));
    }

    @Test
    void failsWhenHostnameAndHostNameAreConfigured() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             hostname: test-host
                                             host-name: test-host-alias
                                             """, MediaTypes.APPLICATION_YAML));

        assertThrows(IllegalArgumentException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    @Test
    void defaultsLocationFromOciEnvConfig() {
        Config rootConfig = Config.just(ConfigSources.create(Map.of(
                "oci.env.availability-domain", "iad-ad-1",
                "oci.env.fault-domain", "2",
                "metrics.publishers.0.type", "oci",
                "metrics.publishers.0.enabled", "true",
                "metrics.publishers.0.project", "test-project",
                "metrics.publishers.0.fleet", "test-fleet")));

        OciMetricsPublisherConfig publisherConfig = publisherConfig(rootConfig);

        assertThat(publisherConfig.availabilityDomain().orElseThrow(), is("iad-ad-1"));
        assertThat(publisherConfig.faultDomain().orElseThrow(), is("2"));
    }

    @Test
    void explicitLocationOverridesOciEnvConfig() {
        Config rootConfig = Config.just(ConfigSources.create(Map.of(
                "oci.env.availability-domain", "iad-ad-1",
                "oci.env.fault-domain", "2",
                "metrics.publishers.0.type", "oci",
                "metrics.publishers.0.enabled", "true",
                "metrics.publishers.0.project", "test-project",
                "metrics.publishers.0.fleet", "test-fleet",
                "metrics.publishers.0.availability-domain", "iad-ad-2",
                "metrics.publishers.0.fault-domain", "3")));

        OciMetricsPublisherConfig publisherConfig = publisherConfig(rootConfig);

        assertThat(publisherConfig.availabilityDomain().orElseThrow(), is("iad-ad-2"));
        assertThat(publisherConfig.faultDomain().orElseThrow(), is("3"));
    }

    @Test
    void defaultsOnlyMissingLocationFromOciEnvConfig() {
        Config rootConfig = Config.just(ConfigSources.create(Map.of(
                "oci.env.availability-domain", "iad-ad-1",
                "oci.env.fault-domain", "2",
                "metrics.publishers.0.type", "oci",
                "metrics.publishers.0.enabled", "true",
                "metrics.publishers.0.project", "test-project",
                "metrics.publishers.0.fleet", "test-fleet",
                "metrics.publishers.0.availability-domain", "iad-ad-2")));

        OciMetricsPublisherConfig publisherConfig = publisherConfig(rootConfig);

        assertThat(publisherConfig.availabilityDomain().orElseThrow(), is("iad-ad-2"));
        assertThat(publisherConfig.faultDomain().orElseThrow(), is("2"));
    }

    @Test
    void buildsExplicitLocationWithoutConfigRoot() {
        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.builder()
                .enabled(true)
                .project("test-project")
                .fleet("test-fleet")
                .availabilityDomain("iad-ad-2")
                .faultDomain("3")
                .defaultDimensions(Map.of())
                .requestHeaders(Map.of())
                .buildPrototype();

        assertThat(publisherConfig.availabilityDomain().orElseThrow(), is("iad-ad-2"));
        assertThat(publisherConfig.faultDomain().orElseThrow(), is("3"));
    }

    private static OciMetricsPublisherConfig publisherConfig() {
        return publisherConfig(CONFIG);
    }

    private static OciMetricsPublisherConfig publisherConfig(Config config) {
        return OciMetricsPublisherConfig.create(config.get("metrics")
                                                       .get("publishers")
                                                       .get("0"));
    }
}
