/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.core.MultivaluedHashMap;

import io.helidon.logging.common.LogConfig;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.FunctionalCounter;
import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.api.Timer;
import io.helidon.service.registry.Services;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.bmc.monitoring.model.Datapoint;
import com.oracle.bmc.monitoring.model.MetricDataDetails;
import com.oracle.bmc.monitoring.model.PostMetricDataResponseDetails;
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest;
import com.oracle.bmc.monitoring.responses.PostMetricDataResponse;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.metrics.OciMetricsPublisherIntegrationTest.DatapointMatchers.hasDataPoints;
import static com.oracle.helidon.oci.metrics.OciMetricsPublisherIntegrationTest.DatapointMatchers.hasDimensions;
import static com.oracle.helidon.oci.metrics.OciMetricsPublisherIntegrationTest.DatapointMatchers.hasName;
import static com.oracle.helidon.oci.metrics.OciMetricsPublisherIntegrationTest.DatapointMatchers.hasValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@ServerTest
class OciMetricsPublisherIntegrationTest {

    private static final String HOST = "test-host";
    private static final String AVAILABILITY_DOMAIN = "iad-ad-1";
    private static final String FAULT_DOMAIN = "1";
    private static final List<MetricDataDetails> CAPTURED_METRICS_DETAILS = new CopyOnWriteArrayList<>();
    private static final Monitoring MONITORING = monitoringCapture();

    private static MeterRegistry meterRegistry;
    private static OciMetricsFactory metricsFactory;

    @SetUpRoute
    static void setUpServices(HttpRouting.Builder builder) {
        LogConfig.configureRuntime();

        Services.set(Monitoring.class, MONITORING);

        // Simplify testing config by setting a region, any region, explicitly.
        Services.set(Region.class, Region.fromPublicRegionName("us-ashburn-1"));

        if (com.oracle.pic.telemetry.commons.metrics.Metrics.isActive()) {
            com.oracle.pic.telemetry.commons.metrics.Metrics.shutdown();
        }

        metricsFactory = (OciMetricsFactory) MetricsFactory.getInstance();
        meterRegistry = metricsFactory.globalRegistry();
    }

    @BeforeEach
    void clearCapturedMetrics() {
        CAPTURED_METRICS_DETAILS.clear();
    }

    @Test
    void checkPublishedMeters() {

        AtomicLong gaugeValue = new AtomicLong(17L);
        AtomicLong functionalCounterValue = new AtomicLong(23L);

        Counter counter = meterRegistry.getOrCreate(Counter.builder("test.counter")
                                                            .addTag(Tag.create("kind", "custom")));
        meterRegistry.getOrCreate(FunctionalCounter.builder("test.functional.counter",
                                                            functionalCounterValue,
                                                            AtomicLong::get)
                                          .addTag(Tag.create("kind", "sampled")));
        Timer timer = meterRegistry.getOrCreate(Timer.builder("test.timer")
                                                        .addTag(Tag.create("operation", "sync")));
        DistributionSummary summary = meterRegistry.getOrCreate(DistributionSummary.builder("test.summary")
                                                                    .addTag(Tag.create("operation", "batch")));
        meterRegistry.getOrCreate(Gauge.builder("test.gauge", gaugeValue::get)
                                          .addTag(Tag.create("kind", "sampled")));

        counter.increment(3);
        timer.record(Duration.ofMillis(25));
        summary.record(13D);

        assertThat("Expected mutations not to report directly to OCI",
                   CAPTURED_METRICS_DETAILS,
                   empty());

        /*
        Normally, meters are measured on a scheduled thread. To avoid test timing issues, trigger sampling now
        rather than trying to wait for a regularly-scheduled periodic sampling to run.
         */
        metricsFactory.runtime().sampleMeters();

        /*
        Force a flush of pending writes.
         */
        Metrics.shutdown();

        assertThat("Expected reported metrics data",
                   CAPTURED_METRICS_DETAILS,
                   allOf(
                           hasItem(allOf(hasName(equalTo("test.counter")),
                                         hasDataPoints(hasItem(hasValue(equalTo(3D)))),
                                         hasDimensions(allOf(hasEntry("kind", "custom"),
                                                             hasEntry("host", HOST),
                                                             hasEntry("availabilityDomain", AVAILABILITY_DOMAIN),
                                                             hasEntry("faultDomain", FAULT_DOMAIN))))),
                           hasItem(allOf(hasName(equalTo("test.functional.counter")),
                                         hasDataPoints(hasItem(hasValue(equalTo(23D)))),
                                         hasDimensions(allOf(hasEntry("kind", "sampled"),
                                                             hasEntry("host", HOST),
                                                             hasEntry("availabilityDomain", AVAILABILITY_DOMAIN),
                                                             hasEntry("faultDomain", FAULT_DOMAIN))))),
                           hasItem(allOf(hasName(equalTo("test.timer")),
                                         hasDataPoints(hasItem(hasValue(equalTo(0D)))),
                                         hasDimensions(allOf(hasEntry("operation", "sync"),
                                                             hasEntry("host", HOST),
                                                             hasEntry("availabilityDomain", AVAILABILITY_DOMAIN),
                                                             hasEntry("faultDomain", FAULT_DOMAIN))))),
                           hasItem(allOf(hasName(equalTo("test.summary")),
                                         hasDataPoints(hasItem(hasValue(equalTo(13D)))),
                                         hasDimensions(allOf(hasEntry("operation", "batch"),
                                                             hasEntry("host", HOST),
                                                             hasEntry("availabilityDomain", AVAILABILITY_DOMAIN),
                                                             hasEntry("faultDomain", FAULT_DOMAIN))))),
                           hasItem(allOf(hasName(equalTo("test.gauge")),
                                         hasDataPoints(hasItem(hasValue(equalTo(17D)))),
                                         hasDimensions(allOf(hasEntry("host", HOST),
                                                             hasEntry("availabilityDomain", AVAILABILITY_DOMAIN),
                                                             hasEntry("faultDomain", FAULT_DOMAIN))))),
                           not(hasItem(hasName(equalTo("test.timer.count")))),
                           not(hasItem(hasName(equalTo("test.timer.total")))),
                           not(hasItem(hasName(equalTo("test.timer.mean")))),
                           not(hasItem(hasName(equalTo("test.timer.max")))),
                           not(hasItem(hasName(equalTo("test.timer.rate")))),
                           not(hasItem(hasName(equalTo("test.summary.count")))),
                           not(hasItem(hasName(equalTo("test.summary.total")))),
                           not(hasItem(hasName(equalTo("test.summary.mean")))),
                           not(hasItem(hasName(equalTo("test.summary.max"))))
                   ));
    }

    /**
     * Matchers useful on MetricDataDetails.
     */
    static class DatapointMatchers {

        private DatapointMatchers() {
        }

        static Matcher<MetricDataDetails> hasName(Matcher<String> nameMatcher) {
            return new FeatureMatcher<>(nameMatcher, "has name", "name") {
                @Override
                protected String featureValueOf(MetricDataDetails actual) {
                    return actual.getName();
                }
            };
        }

        static Matcher<MetricDataDetails> hasDataPoints(Matcher<Iterable<? super Datapoint>> datapointMatcher) {
            return new FeatureMatcher<>(datapointMatcher, "has data points", "dataPoints") {
                @Override
                protected List<Datapoint> featureValueOf(MetricDataDetails actual) {
                    return actual.getDatapoints();
                }
            };
        }

        static Matcher<Datapoint> hasValue(Matcher<Double> valueMatcher) {
            return new FeatureMatcher<>(valueMatcher, "has value", "value") {
                @Override
                protected Double featureValueOf(Datapoint actual) {
                    return actual.getValue();
                }
            };
        }

        @SuppressWarnings("unchecked")
        static <K, V> Matcher<MetricDataDetails> hasDimensions(Matcher<Map<? extends K, ? extends V>> mapMatcher) {
            return new FeatureMatcher<>(mapMatcher, "has dimension", "dimension") {
                @Override
                protected Map<K, V> featureValueOf(MetricDataDetails actual) {
                    return (Map<K, V>) actual.getDimensions();
                }
            };
        }
    }

    private static Monitoring monitoringCapture() {
        return (Monitoring) Proxy.newProxyInstance(Monitoring.class.getClassLoader(),
                                                   new Class<?>[] {Monitoring.class},
                                                   (proxy, method, args) -> {
                                                       if ("postMetricData".equals(method.getName())) {
                                                           PostMetricDataRequest request = (PostMetricDataRequest) args[0];
                                                           CAPTURED_METRICS_DETAILS.addAll(request.getPostMetricDataDetails()
                                                                                                  .getMetricData());
                                                           return PostMetricDataResponse.builder()
                                                                   .__httpStatusCode__(200)
                                                                   .headers(new MultivaluedHashMap<>())
                                                                   .opcRequestId("test-request")
                                                                   .postMetricDataResponseDetails(PostMetricDataResponseDetails.builder()
                                                                                                          .failedMetricsCount(0)
                                                                                                          .failedMetrics(List.of())
                                                                                                          .build())
                                                                   .build();
                                                       }
                                                       return defaultValue(method.getReturnType());
                                                   });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == void.class) {
            return null;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }
}
