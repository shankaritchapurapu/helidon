/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.common.Errors;
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
import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.model.TimeSeries;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OciMetricsPublisherConfigMappingTest {

    private static final Config CONFIG = Config.just(
            ConfigSources.create("""
                                         metrics:
                                           publishers:
                                             - type: oci
                                               enabled: true
                                               reporter:
                                                 overlay:
                                                   project: test-project
                                                   fleet: test-fleet
                                                   region: us-ashburn-1
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
                                               enable-detailed-timing-auto-metrics: false
                                               resource-package-prefix: com.example.store
                                               sample-interval: PT7S
                                               metrics-scope-name: custom-service
                                               includes:
                                                 - test.counter
                                                 - test.timer
                                               excludes:
                                                 - test.excluded
                                               filter-matching-mode: regex
                                               includes-attributes:
                                                 - value
                                                 - count
                                               excludes-attributes:
                                                 - p999
                                               accumulators:
                                                 max-pending-seconds: 12
                                                 max-raw-timer-samples-per-second: 2048
                                                 max-raw-summary-samples-per-second: 4096
                                                 pressure-log-interval: PT15S
                                               auto-http:
                                                 enabled: false
                                                 user-agent-metrics-enabled: false
                                                 max-user-agent-series: 321
                                                 runtime-dimension:
                                                   property-name: lab-environment
                                                   dimension-name: lab
                                                   default-dimension: PINTLAB
                                         """, MediaTypes.APPLICATION_YAML));

    @Test
    void mapsPublisherYamlToReporterClientConfiguration() {
        OciMetricsPublisherConfig publisherConfig = publisherConfig();
        OciMetricReporterConfig reporterConfig = publisherConfig.reporterConfig();
        ClientConfiguration clientConfiguration = reporterConfig.client().orElseThrow();

        assertThat(reporterConfig.project().orElseThrow(), is("test-project"));
        assertThat(reporterConfig.fleet().orElseThrow(), is("test-fleet"));
        assertThat(reporterConfig.region().orElseThrow(), is("us-ashburn-1"));
        assertThat(publisherConfig.resourcePackagePrefix().orElseThrow(), is("com.example.store"));
        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is(7000));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is(11000));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(13));
    }

    @Test
    void defaultsReporterToOverlay() {
        OciMetricsPublisherConfig publisherConfig = publisherConfig();

        assertThat(publisherConfig.reporterConfig(), instanceOf(OverlayMetricReporterConfig.class));
        assertThat(publisherConfig.reporterConfig().type(), is(OciMetricReporterType.OVERLAY.configKey()));
        assertThat(publisherConfig.reporter(), instanceOf(MetricReporter.class));
    }

    @Test
    void mapsExplicitOverlayReporter() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             reporter:
                                               overlay:
                                                 project: test-project
                                                 fleet: test-fleet
                                                 endpoint: https://telemetry.example.com
                                                 hostname: test-host
                                                 availability-domain: iad-ad-1
                                                 fault-domain: 1
                                                 request-headers:
                                                   test-header: test-value
                                                 use-metadata-service: false
                                                 override-metric-keys: true
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);
        OciMetricReporterConfig reporterConfig = publisherConfig.reporterConfig();

        assertThat(reporterConfig, instanceOf(OverlayMetricReporterConfig.class));
        assertThat(reporterConfig.type(), is(OciMetricReporterType.OVERLAY.configKey()));
        assertThat(reporterConfig.project().orElseThrow(), is("test-project"));
        assertThat(reporterConfig.fleet().orElseThrow(), is("test-fleet"));
        assertThat(reporterConfig.endpoint().orElseThrow().toString(), is("https://telemetry.example.com"));
        assertThat(reporterConfig.hostname().orElseThrow(), is("test-host"));
        assertThat(reporterConfig.availabilityDomain().orElseThrow(), is("iad-ad-1"));
        assertThat(reporterConfig.faultDomain().orElseThrow(), is("1"));
        OverlayMetricReporterConfig overlayConfig = (OverlayMetricReporterConfig) reporterConfig;
        assertThat(overlayConfig.requestHeaders(), is(Map.of("test-header", "test-value")));
        assertThat(overlayConfig.useMetadataService().orElseThrow(), is(false));
        assertThat(overlayConfig.overrideMetricKeys().orElseThrow(), is(true));
    }

    @Test
    void mapsExplicitSubstrateReporter() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             reporter:
                                               substrate:
                                                 project: test-project
                                                 fleet: test-fleet
                                                 endpoint: https://t2.example.com
                                                 hostname: test-host
                                                 availability-domain: iad-ad-1
                                                 fault-domain: 1
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);
        OciMetricReporterConfig reporterConfig = publisherConfig.reporterConfig();

        assertThat(reporterConfig, instanceOf(SubstrateMetricReporterConfig.class));
        assertThat(reporterConfig.type(), is(OciMetricReporterType.SUBSTRATE.configKey()));
        assertThat(reporterConfig.project().orElseThrow(), is("test-project"));
        assertThat(reporterConfig.fleet().orElseThrow(), is("test-fleet"));
        assertThat(reporterConfig.endpoint().orElseThrow().toString(), is("https://t2.example.com"));
        assertThat(reporterConfig.hostname().orElseThrow(), is("test-host"));
        assertThat(reporterConfig.availabilityDomain().orElseThrow(), is("iad-ad-1"));
        assertThat(reporterConfig.faultDomain().orElseThrow(), is("1"));
    }

    @Test
    void mapsReporterHostNameAlias() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             reporter:
                                               overlay:
                                                 project: test-project
                                                 fleet: test-fleet
                                                 host-name: alias-host
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);

        assertThat(publisherConfig.reporterConfig().hostname().orElseThrow(), is("alias-host"));
    }

    @Test
    void mapsProgrammaticReporterHostNameAlias() {
        OverlayMetricReporterConfig reporterConfig = OverlayMetricReporterConfig.builder()
                .project("test-project")
                .fleet("test-fleet")
                .hostName("alias-host")
                .build();

        assertThat(reporterConfig.hostname().orElseThrow(), is("alias-host"));
    }

    @Test
    void rejectsReporterHostNameAliasConflict() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             reporter:
                                               overlay:
                                                 project: test-project
                                                 fleet: test-fleet
                                                 hostname: canonical-host
                                                 host-name: alias-host
                                             """, MediaTypes.APPLICATION_YAML));

        assertThrows(ConfigException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    @Test
    void mapsLocationDefaultsFromRootOciEnvironment() {
        Config config = Config.just(
                ConfigSources.create("""
                                             oci:
                                               env:
                                                 availability-domain: root-ad
                                                 fault-domain: root-fd
                                             metrics:
                                               publishers:
                                                 - type: oci
                                                   reporter:
                                                     substrate:
                                                       project: test-project
                                                       fleet: test-fleet
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricReporterConfig reporterConfig = publisherConfig(config).reporterConfig();

        assertThat(reporterConfig.availabilityDomain().orElseThrow(), is("root-ad"));
        assertThat(reporterConfig.faultDomain().orElseThrow(), is("root-fd"));
    }

    @Test
    void nestedLocationSettingsOverrideRootOciEnvironment() {
        Config config = Config.just(
                ConfigSources.create("""
                                             oci:
                                               env:
                                                 availability-domain: root-ad
                                                 fault-domain: root-fd
                                             metrics:
                                               publishers:
                                                 - type: oci
                                                   reporter:
                                                     overlay:
                                                       project: test-project
                                                       fleet: test-fleet
                                                       availability-domain: nested-ad
                                                       fault-domain: nested-fd
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricReporterConfig reporterConfig = publisherConfig(config).reporterConfig();

        assertThat(reporterConfig.availabilityDomain().orElseThrow(), is("nested-ad"));
        assertThat(reporterConfig.faultDomain().orElseThrow(), is("nested-fd"));
    }

    @Test
    void metricTimeSeriesClientIsProgrammaticOnly() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             reporter:
                                               substrate:
                                                 project: test-project
                                                 fleet: test-fleet
                                                 metric-time-series-client: ignored
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);
        SubstrateMetricReporterConfig reporterConfig = (SubstrateMetricReporterConfig) publisherConfig.reporterConfig();

        assertThat(reporterConfig.metricTimeSeriesClient(), is(java.util.Optional.empty()));
    }

    @Test
    void failsForInvalidReporterType() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             reporter:
                                               unsupported: {}
                                             """, MediaTypes.APPLICATION_YAML));

        assertThrows(ConfigException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    @Test
    void reporterTypeIsNotASelector() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             reporter:
                                               type: substrate
                                             """, MediaTypes.APPLICATION_YAML));

        assertThrows(ConfigException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    @Test
    void rejectsAllBlankAutoHttpRuntimeDimensionSettings() {
        Config config = Config.just(ConfigSources.create("""
                                                                 type: oci
                                                                 auto-http:
                                                                   runtime-dimension:
                                                                     property-name: " "
                                                                     dimension-name: " "
                                                                 """, MediaTypes.APPLICATION_YAML));

        Errors.ErrorMessagesException exception = assertThrows(Errors.ErrorMessagesException.class,
                                                               () -> OciMetricsPublisherConfig.create(config));

        assertThat(exception.getMessage(),
                   containsString("auto-http.runtime-dimension.property-name must be configured."));
        assertThat(exception.getMessage(),
                   containsString("auto-http.runtime-dimension.dimension-name must be configured."));
    }

    @Test
    void programmaticReporterWinsOverConfiguredReporterType() {
        MetricReporter reporter = new TestMetricReporter();

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.builder()
                .config(Config.just(ConfigSources.create("""
                                                                 type: oci
                                                                 reporter:
                                                                   substrate:
                                                                     project: configured-project
                                                                 """, MediaTypes.APPLICATION_YAML)))
                .reporter(reporter)
                .buildPrototype();

        assertThat(publisherConfig.reporter(), sameInstance(reporter));
    }

    @Test
    void mapsPublisherYamlToRetryConfiguration() {
        ClientConfiguration clientConfiguration = publisherConfig().reporterConfig().client().orElseThrow();
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
        ClientConfiguration clientConfiguration = publisherConfig().reporterConfig().client().orElseThrow();
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

        assertThat(publisherConfig.sampleInterval(), is(Duration.ofSeconds(7)));
        assertThat(publisherConfig.metricsScopeName(), is("custom-service"));
        assertThat(publisherConfig.includes(), is(Set.of("test.counter", "test.timer")));
        assertThat(publisherConfig.excludes(), is(Set.of("test.excluded")));
        assertThat(publisherConfig.filterMatchingMode(), is(FilterMatchingMode.REGEX));
        assertThat(publisherConfig.includesAttributes(), is(Set.of("value", "count")));
        assertThat(publisherConfig.excludesAttributes(), is(Set.of("p999")));
        assertThat(publisherConfig.filter(), instanceOf(ReporterMetricFilter.class));
    }

    @Test
    void mapsPublisherYamlToAccumulatorConfig() {
        OciMetricsAccumulatorConfig accumulators = publisherConfig().accumulators();

        assertThat(accumulators.maxPendingSeconds(), is(12));
        assertThat(accumulators.maxRawTimerSamplesPerSecond(), is(2048));
        assertThat(accumulators.maxRawSummarySamplesPerSecond(), is(4096));
        assertThat(accumulators.pressureLogInterval(), is(Duration.ofSeconds(15)));
    }

    @Test
    void mapsPublisherYamlToSubstringMatchingConfig() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             filter-matching-mode: substring
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);

        assertThat(publisherConfig.filterMatchingMode(), is(FilterMatchingMode.SUBSTRING));
    }

    @Test
    void defaultsPublisherYamlToExactMatchingConfig() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);

        assertThat(publisherConfig.filterMatchingMode(), is(FilterMatchingMode.EXACT));
        assertThat(publisherConfig.includesAttributes(), is(Set.of("value")));
        assertThat(publisherConfig.sampleInterval(), is(Duration.ofSeconds(1)));
        assertThat(publisherConfig.accumulators().maxPendingSeconds(), is(10));
        assertThat(publisherConfig.accumulators().maxRawTimerSamplesPerSecond(), is(1024));
        assertThat(publisherConfig.accumulators().maxRawSummarySamplesPerSecond(), is(1024));
        assertThat(publisherConfig.accumulators().pressureLogInterval(), is(Duration.ofSeconds(30)));
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
    void mapsPublisherYamlToAutoHttpConfig() {
        OciAutoHttpMetricsConfig autoHttp = publisherConfig().autoHttp();

        assertThat(autoHttp.enabled(), is(false));
        assertThat(autoHttp.userAgentMetricsEnabled(), is(false));
        assertThat(autoHttp.maxUserAgentSeries(), is(321));
        OciAutoHttpRuntimeDimensionConfig runtimeDimension = autoHttp.runtimeDimension().orElseThrow();
        assertThat(runtimeDimension.propertyName(), is("lab-environment"));
        assertThat(runtimeDimension.dimensionName(), is("lab"));
        assertThat(runtimeDimension.defaultDimension().orElseThrow(), is("PINTLAB"));
    }

    @Test
    void defaultsAutoHttpConfig() {
        OciAutoHttpMetricsConfig autoHttp = OciMetricsPublisherConfig.create(Config.just(
                ConfigSources.create("type: oci", MediaTypes.APPLICATION_YAML))).autoHttp();

        assertThat(autoHttp.enabled(), is(true));
        assertThat(autoHttp.userAgentMetricsEnabled(), is(true));
        assertThat(autoHttp.maxUserAgentSeries(), is(1000));
        assertThat(autoHttp.runtimeDimension(), is(java.util.Optional.empty()));
    }

    @Test
    void rejectsBlankAutoHttpRuntimeDimensionPropertyName() {
        Config config = Config.just(ConfigSources.create("""
                                                                 type: oci
                                                                 auto-http:
                                                                   runtime-dimension:
                                                                     property-name: " "
                                                                     dimension-name: lab
                                                                 """, MediaTypes.APPLICATION_YAML));

        assertThrows(RuntimeException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    @Test
    void rejectsNonPositiveUserAgentSeriesLimit() {
        Config config = Config.just(ConfigSources.create("""
                                                                 type: oci
                                                                 auto-http:
                                                                   max-user-agent-series: 0
                                                                 """, MediaTypes.APPLICATION_YAML));

        assertThrows(RuntimeException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    @Test
    void rejectsBlankAutoHttpRuntimeDimensionDimensionName() {
        Config config = Config.just(ConfigSources.create("""
                                                                 type: oci
                                                                 auto-http:
                                                                   runtime-dimension:
                                                                     property-name: lab-environment
                                                                     dimension-name: " "
                                                                 """, MediaTypes.APPLICATION_YAML));

        assertThrows(RuntimeException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    @Test
    void rejectsNonPositiveSampleInterval() {
        assertInvalidPublisherConfig("""
                                             type: oci
                                             sample-interval: PT0S
                                             """);
    }

    @Test
    void rejectsSubMillisecondSampleInterval() {
        assertInvalidPublisherConfig("""
                                             type: oci
                                             sample-interval: PT0.000000001S
                                             """);
    }

    @Test
    void acceptsOneMillisecondSampleInterval() {
        Config config = Config.just(
                ConfigSources.create("""
                                             type: oci
                                             sample-interval: PT0.001S
                                             """, MediaTypes.APPLICATION_YAML));

        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.create(config);

        assertThat(publisherConfig.sampleInterval(), is(Duration.ofMillis(1)));
    }

    @Test
    void rejectsNonPositiveMaxPendingSeconds() {
        assertInvalidPublisherConfig("""
                                             type: oci
                                             accumulators:
                                               max-pending-seconds: 0
                                             """);
    }

    @Test
    void rejectsNonPositiveMaxRawTimerSamplesPerSecond() {
        assertInvalidPublisherConfig("""
                                             type: oci
                                             accumulators:
                                               max-raw-timer-samples-per-second: 0
                                             """);
    }

    @Test
    void rejectsNonPositiveMaxRawSummarySamplesPerSecond() {
        assertInvalidPublisherConfig("""
                                             type: oci
                                             accumulators:
                                               max-raw-summary-samples-per-second: 0
                                             """);
    }

    @Test
    void rejectsNonPositivePressureLogInterval() {
        assertInvalidPublisherConfig("""
                                             type: oci
                                             accumulators:
                                               pressure-log-interval: PT0S
                                             """);
    }

    private static OciMetricsPublisherConfig publisherConfig() {
        return publisherConfig(CONFIG);
    }

    private static OciMetricsPublisherConfig publisherConfig(Config config) {
        return OciMetricsPublisherConfig.create(config.get("metrics")
                                                       .get("publishers")
                                                       .get("0"));
    }

    private static void assertInvalidPublisherConfig(String yaml) {
        Config config = Config.just(ConfigSources.create(yaml, MediaTypes.APPLICATION_YAML));

        assertThrows(RuntimeException.class, () -> OciMetricsPublisherConfig.create(config));
    }

    private static final class TestMetricReporter implements MetricReporter {
        @Override
        public void send(List<TimeSeries> timeSeries) {
        }

        @Override
        public void stop() {
        }
    }
}
