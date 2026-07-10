/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import io.helidon.common.context.Context;
import io.helidon.config.Config;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Method;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.Status;
import io.helidon.http.WritableHeaders;
import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.helidon.webserver.observe.metrics.MetricsObserverConfig;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.model.TimeSeries;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class OciMetricsSemanticConventionsTest {
    private static final String MATCHING_RESOURCE_CLASS = "com.example.store.StoreEndpoint";
    private static final String OTHER_RESOURCE_CLASS = "com.example.other.OtherEndpoint";
    private static final String RUNTIME_PROPERTY = "lab-environment";
    private static final String RUNTIME_DIMENSION = "lab";

    @Test
    void emitsServiceCoreNamesForGeneratedEndpointContext() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));

        recordRequest(harness, "/store/items/one", Status.OK_200);

        assertCoreMeters(harness, "StoreEndpoint.get", 200, "2XX", 1.0, true);
    }

    @Test
    void emitsSameMetricsForSecondaryScopes() {
        MetricsTestHarness harness = MetricsTestHarness.create(
                new OciHttpEndpointMetricsContext("StoreEndpoint.list",
                                                  List.of("StoreApi", "ReadApi"),
                                                  MATCHING_RESOURCE_CLASS));

        recordRequest(harness, "/store/items/one", Status.FORBIDDEN_403);

        assertCoreMeters(harness, "StoreEndpoint.list", 403, "4XX", 1.0, true);
        assertCoreMeters(harness, "StoreApi", 403, "4XX", 1.0, true);
        assertCoreMeters(harness, "ReadApi", 403, "4XX", 1.0, true);
    }

    @Test
    void fallsBackToUnknownMethodWithoutGeneratedEndpointContext() {
        MetricsTestHarness harness = MetricsTestHarness.create(null);

        recordRequest(harness, "/bad", Status.NOT_FOUND_404);

        assertCoreMeters(harness, "UnknownMethod", 404, "4XX", 1.0, false);
        assertMetricAbsent(harness, "UnknownMethod.ResourceTime");
    }

    @Test
    void successRateRecordsZeroForServerErrors() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.delete"));

        recordRequest(harness, "/store/items/one", Status.INTERNAL_SERVER_ERROR_500);

        assertCoreMeters(harness, "StoreEndpoint.delete", 500, "5XX", 0.0, true);
    }

    @Test
    void requestBodyReadEmitsWireReadTime() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.post"));

        recordRequest(harness, "/store/items", Status.OK_200, true, false);

        assertMetricPresent(harness, "StoreEndpoint.post.WireReadTime");
        assertMetricAbsent(harness, "StoreEndpoint.post.WireWriteTime");
    }

    @Test
    void noRequestBodyReadEmitsNoWireReadTime() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));

        recordRequest(harness, "/store/items/one", Status.OK_200, false, false);

        assertMetricAbsent(harness, "StoreEndpoint.get.WireReadTime");
    }

    @Test
    void responseBodyWriteEmitsWireWriteTime() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));

        recordRequest(harness, "/store/items/one", Status.OK_200, false, true);

        assertMetricPresent(harness, "StoreEndpoint.get.WireWriteTime");
        assertMetricAbsent(harness, "StoreEndpoint.get.WireReadTime");
    }

    @Test
    void responseBodyWriteEmitsWireWriteTimeOnFrameworkManagedCompletion() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));

        recordRequest(harness, "/store/items/one", Status.OK_200, false, new ByteArrayOutputStream(), false);

        assertMetricPresent(harness, "StoreEndpoint.get.WireWriteTime");
    }

    @Test
    void responseStreamFilterComposesWithExistingFilter() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));
        AtomicInteger existingFilterWrites = new AtomicInteger();
        harness.responseStreamFilter().set(stream -> new FilterOutputStream(stream) {
            @Override
            public void write(byte[] bytes, int offset, int length) throws java.io.IOException {
                existingFilterWrites.incrementAndGet();
                out.write(bytes, offset, length);
            }
        });

        recordRequest(harness, "/store/items/one", Status.OK_200, false, true);

        assertThat(existingFilterWrites.get(), is(1));
        assertMetricPresent(harness, "StoreEndpoint.get.WireWriteTime");
    }

    @Test
    void responseBodyWritePreservesBulkWrites() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));
        CountingOutputStream outputStream = new CountingOutputStream();

        recordRequest(harness, "/store/items/one", Status.OK_200, outputStream);

        assertThat(outputStream.bulkWriteCount(), is(1));
        assertThat(outputStream.singleByteWriteCount(), is(0));
    }

    @Test
    void emptyResponseEmitsNoWireWriteTime() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.delete"));

        recordRequest(harness, "/store/items/one", Status.NO_CONTENT_204, false, false);

        assertMetricAbsent(harness, "StoreEndpoint.delete.WireWriteTime");
    }

    @Test
    void wireTimersUseSecondaryScopes() {
        MetricsTestHarness harness = MetricsTestHarness.create(
                new OciHttpEndpointMetricsContext("StoreEndpoint.put",
                                                  List.of("StoreApi", "WriteApi"),
                                                  MATCHING_RESOURCE_CLASS));

        recordRequest(harness, "/store/items/one", Status.OK_200, true, true);

        assertMetricPresent(harness, "StoreEndpoint.put.WireReadTime");
        assertMetricPresent(harness, "StoreEndpoint.put.WireWriteTime");
        assertMetricPresent(harness, "StoreApi.WireReadTime");
        assertMetricPresent(harness, "StoreApi.WireWriteTime");
        assertMetricPresent(harness, "WriteApi.WireReadTime");
        assertMetricPresent(harness, "WriteApi.WireWriteTime");
    }

    @Test
    void wireTimersFallBackToUnknownMethodWithoutGeneratedEndpointContext() {
        MetricsTestHarness harness = MetricsTestHarness.create(null);

        recordRequest(harness, "/unknown", Status.NOT_FOUND_404, true, true);

        assertMetricPresent(harness, "UnknownMethod.WireReadTime");
        assertMetricPresent(harness, "UnknownMethod.WireWriteTime");
    }

    @Test
    void disabledDetailedTimingSuppressesTimingOnly() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.disabledTiming"), false);

        recordRequest(harness, "/store/items", Status.OK_200, true, true);

        assertMetricAbsent(harness, "StoreEndpoint.disabledTiming.Time");
        assertMetricAbsent(harness, "StoreEndpoint.disabledTiming.ResourceTime");
        assertMetricAbsent(harness, "StoreEndpoint.disabledTiming.WireReadTime");
        assertMetricAbsent(harness, "StoreEndpoint.disabledTiming.WireWriteTime");
        assertMetricValue(harness, "StoreEndpoint.disabledTiming.ResponseOut.StatusCode.200.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.disabledTiming.ResponseOut.StatusFamily.2XX.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.disabledTiming.ResponseOut.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.disabledTiming.SuccessRate", 1.0);
    }

    @Test
    void nullPublisherPrototypeFallsBackToDefaultAutoMetricsConfig() {
        MetricsTestHarness harness = MetricsTestHarness.createWithNullPublisherPrototype(endpoint("StoreEndpoint.nullPrototype"));

        recordRequest(harness, "/store/items", Status.OK_200, true, true);

        assertCoreMeters(harness, "StoreEndpoint.nullPrototype", 200, "2XX", 1.0, true);
        assertMetricPresent(harness, "StoreEndpoint.nullPrototype.WireReadTime");
        assertMetricPresent(harness, "StoreEndpoint.nullPrototype.WireWriteTime");
    }

    @Test
    void resourcePackagePrefixAllowsMatchingResource() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.filtered"),
                                                               true,
                                                               Optional.of("com.example.store"));

        recordRequest(harness, "/store/items", Status.OK_200, true, true);

        assertCoreMeters(harness, "StoreEndpoint.filtered", 200, "2XX", 1.0, true);
        assertMetricPresent(harness, "StoreEndpoint.filtered.WireReadTime");
        assertMetricPresent(harness, "StoreEndpoint.filtered.WireWriteTime");
    }

    @Test
    void resourcePackagePrefixSuppressesNonMatchingResource() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.filteredOut", OTHER_RESOURCE_CLASS),
                                                               true,
                                                               Optional.of("com.example.store"));

        recordRequest(harness, "/other/items", Status.OK_200, true, true);

        assertRequestProceeded(harness);
        assertThat(harness.addCount().get(), is(0));
        assertMetricAbsent(harness, "StoreEndpoint.filteredOut.Time");
        assertMetricAbsent(harness, "StoreEndpoint.filteredOut.WireReadTime");
        assertMetricAbsent(harness, "StoreEndpoint.filteredOut.WireWriteTime");
    }

    @Test
    void resourcePackagePrefixSuppressesUnknownMethod() {
        MetricsTestHarness harness = MetricsTestHarness.create(null, true, Optional.of("com.example.store"));

        recordRequest(harness, "/unknown", Status.NOT_FOUND_404, true, true);

        assertRequestProceeded(harness);
        assertThat(harness.addCount().get(), is(0));
        assertMetricAbsent(harness, "UnknownMethod.Time");
        assertMetricAbsent(harness, "UnknownMethod.WireReadTime");
        assertMetricAbsent(harness, "UnknownMethod.WireWriteTime");
    }

    @Test
    void metricNameFilterAppliesToAutomaticHttpMetrics() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.included"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.includes(
                                                                       java.util.Set.of("StoreEndpoint.included.ResponseOut.Count")));

        recordRequest(harness, "/store/items", Status.OK_200);

        assertMetricValue(harness, "StoreEndpoint.included.ResponseOut.Count", 1.0);
        assertMetricAbsent(harness, "StoreEndpoint.included.Time");
        assertMetricAbsent(harness, "StoreEndpoint.included.ResponseOut.StatusCode.200.Count");
        assertMetricAbsent(harness, "StoreEndpoint.included.SuccessRate");
        assertMetricAbsent(harness, "StoreEndpoint.included.Request.Client.UNDEFINED.Count");
    }

    @Test
    void attributeFilterAppliesToAutomaticHttpTimerDurationSamples() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.valueFiltered"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.includes(
                                                                               java.util.Set.of(
                                                                                       "StoreEndpoint.valueFiltered.Time"))
                                                                       .excludesAttributes(
                                                                               java.util.Set.of(
                                                                                       OciMetricsPublisher.VALUE_ATTRIBUTE)));

        recordRequest(harness, "/store/items", Status.OK_200);

        assertMetricAbsent(harness, "StoreEndpoint.valueFiltered.Time");
    }

    @Test
    void helidonMeterEnablementSuppressesSpecificAutomaticHttpMetric() {
        MetricsTestHarness harness = MetricsTestHarness.createWithMetricsConfig(
                endpoint("StoreEndpoint.disabledByMetrics"),
                builder -> builder.scoping(scoping -> scoping.putScope(
                        Meter.Scope.DEFAULT,
                        scope -> scope.name(Meter.Scope.DEFAULT)
                                .exclude("StoreEndpoint\\.disabledByMetrics\\.Time"))));

        recordRequest(harness, "/store/items", Status.OK_200);

        assertMetricAbsent(harness, "StoreEndpoint.disabledByMetrics.Time");
        assertMetricValue(harness, "StoreEndpoint.disabledByMetrics.ResponseOut.Count", 1.0);
    }

    @Test
    void automaticHttpMetricsRegisterHelidonMeters() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.meters"));

        recordRequest(harness, "/store/items", Status.OK_200, true, true);

        assertMetricPresent(harness, "StoreEndpoint.meters.Time");
        assertFalse(harness.registry().meters().isEmpty());
        assertThat(harness.addCount().get(), is(10));
    }

    @Test
    void disabledOciAutoHttpMetricsInstallsNoFilter() {
        assertFalse(MetricsTestHarness.filterPresent(builder -> builder
                .autoHttp(OciAutoHttpMetricsConfig.builder()
                                  .enabled(false)
                                  .build())));
    }

    @Test
    void userAgentMetricsUseParsedServiceCoreAggregations() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.userAgent"));

        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/3.1.2 (Mac OS X/13.0; Java/17.0.4; vendor)");

        assertMetricValue(harness, "StoreEndpoint.userAgent.Request.Client.JavaSDK.2XX.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.userAgent.Request.Client.JavaSDK.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.userAgent.Request.Client.JavaSDK.312.macosx.130.java.1704.2XX.Count",
                          1.0);
        assertMetricValue(harness, "StoreEndpoint.userAgent.Request.Client.JavaSDK.312.macosx.130.java.1704.Count",
                          1.0);
    }

    @Test
    void userAgentMetricsCollapseDetailedSeriesAfterCardinalityLimit() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.userAgentBounded"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(
                                                                       OciAutoHttpMetricsConfig.builder()
                                                                               .maxUserAgentSeries(2)
                                                                               .build()));

        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/1 (Mac OS X/13.0; Java/17.0.4; vendor)");
        harness.awaitAsyncMetrics();
        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/2 (Mac OS X/13.0; Java/17.0.4; vendor)");

        assertMetricObservationCount(harness,
                                     "StoreEndpoint.userAgentBounded.Request.Client.JavaSDK.Count",
                                     2L);
        String version1Metric = "StoreEndpoint.userAgentBounded.Request.Client.JavaSDK.1.Count";
        String version2Metric = "StoreEndpoint.userAgentBounded.Request.Client.JavaSDK.2.Count";
        List<TimeSeries> flushedMetrics = harness.flushMetrics();
        boolean version1Present = hasMetric(flushedMetrics, version1Metric);
        boolean version2Present = hasMetric(flushedMetrics, version2Metric);
        assertTrue(version1Present ^ version2Present,
                   () -> "Expected exactly one detailed user-agent metric in " + flushedMetrics);
        String admittedVersion = version1Present ? "1" : "2";
        String overflowedMetric = version1Present ? version2Metric : version1Metric;
        assertMetricAbsent(harness, overflowedMetric);
        assertMetricObservationCount(harness,
                                     "StoreEndpoint.userAgentBounded.Request.Client.OTHER.Count",
                                     2L);
        assertMetricObservationCount(harness,
                                     OciMetricsPublisher.USER_AGENT_CARDINALITY_OVERFLOWS,
                                     2L);
        List<String> admittedNames = harness.registry().meters().stream()
                .map(meter -> meter.id().name())
                .filter(name -> name.contains("Request.Client.JavaSDK." + admittedVersion + "."))
                .toList();
        assertThat(admittedNames.toString(), admittedNames.size(), is(2));
    }

    @Test
    void excludedUserAgentSeriesDoNotConsumeCardinalityOrRegisterMeters() {
        String scope = "StoreEndpoint.userAgentExcluded";
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint(scope),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder
                                                                       .filterMatchingMode(FilterMatchingMode.SUBSTRING)
                                                                       .excludes(Set.of(scope + ".Request.Client"))
                                                                       .autoHttp(OciAutoHttpMetricsConfig.builder()
                                                                                         .maxUserAgentSeries(2)
                                                                                         .build()));

        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/1 (Mac OS X/13.0; Java/17.0.4; vendor)");
        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/2 (Mac OS X/13.0; Java/17.0.4; vendor)");

        assertThat(harness.registry().meters().stream()
                           .map(meter -> meter.id().name())
                           .noneMatch(name -> name.contains("Request.Client")),
                   is(true));
        assertMetricAbsent(harness, scope + ".Request.Client.OTHER.Count");
        assertMetricAbsent(harness, OciMetricsPublisher.USER_AGENT_CARDINALITY_OVERFLOWS);
    }

    @Test
    void userAgentMetricsUseUndefinedForMissingHeader() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.undefinedUserAgent"));

        recordRequest(harness, "/store/items", Status.OK_200);

        assertMetricValue(harness, "StoreEndpoint.undefinedUserAgent.Request.Client.UNDEFINED.2XX.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.undefinedUserAgent.Request.Client.UNDEFINED.Count", 1.0);
    }

    @Test
    void userAgentMetricsUseUnknownForUnparsedHeader() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.unknownUserAgent"));

        recordRequest(harness, "/store/items", Status.OK_200, "not-a-known-client/1.0");

        assertMetricValue(harness, "StoreEndpoint.unknownUserAgent.Request.Client.UNKNOWN.2XX.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.unknownUserAgent.Request.Client.UNKNOWN.Count", 1.0);
    }

    @Test
    void disabledUserAgentMetricsSuppressesClientCountersOnly() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.noUserAgent"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(
                                                                       OciAutoHttpMetricsConfig.builder()
                                                                               .userAgentMetricsEnabled(false)
                                                                               .build()));

        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/3.1.2 (Mac OS X/13.0; Java/17.0.4; vendor)");

        assertCoreMeters(harness, "StoreEndpoint.noUserAgent", 200, "2XX", 1.0, true);
        assertMetricAbsent(harness, "StoreEndpoint.noUserAgent.Request.Client.JavaSDK.Count");
        assertMetricAbsent(harness, "StoreEndpoint.noUserAgent.Request.Client.UNDEFINED.Count");
    }

    @Test
    void skipResponseStatusMetricsSuppressesOnlyStatusDependentMetrics() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.skipStatus"));

        recordRequest(harness,
                      "/store/items",
                      Status.NOT_FOUND_404,
                      "Oracle-JavaSDK/3.1.2 (Mac OS X/13.0; Java/17.0.4; vendor)",
                      true);

        assertMetricAbsent(harness, "StoreEndpoint.skipStatus.ResponseOut.StatusCode.404.Count");
        assertMetricAbsent(harness, "StoreEndpoint.skipStatus.ResponseOut.StatusFamily.4XX.Count");
        assertMetricAbsent(harness, "StoreEndpoint.skipStatus.Request.Client.JavaSDK.4XX.Count");
        assertMetricValue(harness, "StoreEndpoint.skipStatus.ResponseOut.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.skipStatus.SuccessRate", 1.0);
        assertMetricValue(harness, "StoreEndpoint.skipStatus.Request.Client.JavaSDK.Count", 1.0);
    }

    @Test
    void runtimeDimensionFromRequestContextAppliesServiceCoreNamesAndDimensions() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.runtime"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(autoHttpRuntimeDimension()));

        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/3.1.2 (Mac OS X/13.0; Java/17.0.4; vendor)",
                      false,
                      "PINTLAB");

        assertMetricDimensions(harness, "StoreEndpoint.runtime.Time", Map.of(RUNTIME_DIMENSION, "PINTLAB"));
        assertMetricDimensions(harness, "StoreEndpoint.runtime.SuccessRate", Map.of(RUNTIME_DIMENSION, "PINTLAB"));
        assertMetricValue(harness, "StoreEndpoint.runtime.PINTLAB.ResponseOut.Count", 1.0);
        assertMetricDimensions(harness,
                               "StoreEndpoint.runtime.PINTLAB.ResponseOut.Count",
                               Map.of(RUNTIME_DIMENSION, "PINTLAB"));
        assertMetricValue(harness, "StoreEndpoint.runtime.PINTLAB.ResponseOut.StatusCode.200.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.runtime.PINTLAB.ResponseOut.StatusFamily.2XX.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.runtime.PINTLAB.Request.Client.JavaSDK.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.runtime.PINTLAB.Request.Client.JavaSDK.2XX.Count", 1.0);
        assertMetricAbsent(harness, "StoreEndpoint.runtime.ResponseOut.Count");
    }

    @Test
    void runtimeDimensionPreservesRawValueInCountMetricNames() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.runtimeRaw"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(autoHttpRuntimeDimension()));

        recordRequest(harness,
                      "/store/items",
                      Status.OK_200,
                      "Oracle-JavaSDK/3.1.2 (Mac OS X/13.0; Java/17.0.4; vendor)",
                      false,
                      "PINT.LAB-1");

        assertMetricValue(harness, "StoreEndpoint.runtimeRaw.PINT.LAB-1.ResponseOut.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.runtimeRaw.PINT.LAB-1.Request.Client.JavaSDK.Count", 1.0);
        assertMetricDimensions(harness,
                               "StoreEndpoint.runtimeRaw.PINT.LAB-1.ResponseOut.Count",
                               Map.of(RUNTIME_DIMENSION, "PINT.LAB-1"));
    }

    @Test
    void runtimeDimensionUsesConfiguredDefaultWhenRequestValueAbsent() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.runtimeDefault"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(autoHttpRuntimeDimension(
                                                                       "PINTLAB")));

        recordRequest(harness, "/store/items", Status.OK_200);

        assertMetricValue(harness, "StoreEndpoint.runtimeDefault.PINTLAB.ResponseOut.Count", 1.0);
        assertMetricDimensions(harness,
                               "StoreEndpoint.runtimeDefault.PINTLAB.ResponseOut.Count",
                               Map.of(RUNTIME_DIMENSION, "PINTLAB"));
        assertMetricDimensions(harness, "StoreEndpoint.runtimeDefault.Time", Map.of(RUNTIME_DIMENSION, "PINTLAB"));
    }

    @Test
    void runtimeDimensionEmitsCurrentNamesWhenNoValueResolves() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.noRuntimeValue"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(autoHttpRuntimeDimension()));

        recordRequest(harness, "/store/items", Status.OK_200);

        assertMetricValue(harness, "StoreEndpoint.noRuntimeValue.ResponseOut.Count", 1.0);
        assertMetricDimensions(harness, "StoreEndpoint.noRuntimeValue.ResponseOut.Count", Map.of());
        assertMetricDimensions(harness, "StoreEndpoint.noRuntimeValue.Time", Map.of());
        assertMetricAbsent(harness, "StoreEndpoint.noRuntimeValue.PINTLAB.ResponseOut.Count");
    }

    @Test
    void runtimeDimensionPreservesSkipResponseStatusSuppression() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.runtimeSkip"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(autoHttpRuntimeDimension()));

        recordRequest(harness,
                      "/store/items",
                      Status.NOT_FOUND_404,
                      "Oracle-JavaSDK/3.1.2 (Mac OS X/13.0; Java/17.0.4; vendor)",
                      true,
                      "PINTLAB");

        assertMetricAbsent(harness, "StoreEndpoint.runtimeSkip.PINTLAB.ResponseOut.StatusCode.404.Count");
        assertMetricAbsent(harness, "StoreEndpoint.runtimeSkip.PINTLAB.ResponseOut.StatusFamily.4XX.Count");
        assertMetricAbsent(harness, "StoreEndpoint.runtimeSkip.PINTLAB.Request.Client.JavaSDK.4XX.Count");
        assertMetricValue(harness, "StoreEndpoint.runtimeSkip.PINTLAB.ResponseOut.Count", 1.0);
        assertMetricValue(harness, "StoreEndpoint.runtimeSkip.PINTLAB.Request.Client.JavaSDK.Count", 1.0);
    }

    @Test
    void runtimeDimensionCreatesDistinctMetersForDistinctValues() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.runtimeCardinality"),
                                                               true,
                                                               Optional.empty(),
                                                               builder -> builder.autoHttp(autoHttpRuntimeDimension()));

        recordRequest(harness, "/store/items", Status.OK_200, null, false, "PINTLAB");
        recordRequest(harness, "/store/items", Status.OK_200, null, false, "PROD");

        assertMetricValue(harness, "StoreEndpoint.runtimeCardinality.PINTLAB.ResponseOut.Count", 1.0);
        assertMetricDimensions(harness,
                               "StoreEndpoint.runtimeCardinality.PINTLAB.ResponseOut.Count",
                               Map.of(RUNTIME_DIMENSION, "PINTLAB"));
        assertMetricValue(harness, "StoreEndpoint.runtimeCardinality.PROD.ResponseOut.Count", 1.0);
        assertMetricDimensions(harness,
                               "StoreEndpoint.runtimeCardinality.PROD.ResponseOut.Count",
                               Map.of(RUNTIME_DIMENSION, "PROD"));
    }

    private static OciHttpEndpointMetricsContext endpoint(String primaryScope) {
        return endpoint(primaryScope, MATCHING_RESOURCE_CLASS);
    }

    private static OciHttpEndpointMetricsContext endpoint(String primaryScope, String fullyQualifiedResourceClassName) {
        return new OciHttpEndpointMetricsContext(primaryScope, List.of(), fullyQualifiedResourceClassName);
    }

    private static void recordRequest(MetricsTestHarness harness, String path, Status status) {
        recordRequest(harness, path, status, false, false);
    }

    private static void recordRequest(MetricsTestHarness harness, String path, Status status, String userAgent) {
        recordRequest(harness, path, status, userAgent, false);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Status status,
                                      String userAgent,
                                      boolean skipResponseStatusMetrics) {
        recordRequest(harness, path, status, userAgent, skipResponseStatusMetrics, null);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Status status,
                                      String userAgent,
                                      boolean skipResponseStatusMetrics,
                                      String runtimeDimensionValue) {
        recordRequest(harness,
                      path,
                      status,
                      false,
                      null,
                      true,
                      userAgent,
                      skipResponseStatusMetrics,
                      runtimeDimensionValue);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Status status,
                                      boolean readRequestBody,
                                      boolean writeResponseBody) {
        recordRequest(harness,
                      path,
                      status,
                      readRequestBody,
                      writeResponseBody ? new ByteArrayOutputStream() : null);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Status status,
                                      OutputStream responseBodyStream) {
        recordRequest(harness, path, status, false, responseBodyStream);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Status status,
                                      boolean readRequestBody,
                                      OutputStream responseBodyStream) {
        recordRequest(harness, path, status, readRequestBody, responseBodyStream, true);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Status status,
                                      boolean readRequestBody,
                                      OutputStream responseBodyStream,
                                      boolean closeResponseBodyStream) {
        recordRequest(harness,
                      path,
                      status,
                      readRequestBody,
                      responseBodyStream,
                      closeResponseBodyStream,
                      null,
                      false,
                      null);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Status status,
                                      boolean readRequestBody,
                                      OutputStream responseBodyStream,
                                      boolean closeResponseBodyStream,
                                      String userAgent,
                                      boolean skipResponseStatusMetrics,
                                      String runtimeDimensionValue) {
        harness.invalidateFlush();
        Context context = Context.create();
        RoutingRequest request = proxy(RoutingRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "prologue" -> HttpPrologue.create("HTTP/1.1", "HTTP", "1.1", Method.GET, path, false);
            case "context" -> context;
            case "headers" -> headers(userAgent);
            case "streamFilter" -> {
                harness.requestStreamFilter().set(compose(harness.requestStreamFilter().get(), streamFilter(args)));
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });

        RoutingResponse response = proxy(RoutingResponse.class, (proxy, method, args) -> switch (method.getName()) {
            case "status" -> status;
            case "whenSent" -> {
                harness.whenSentHandler().set((Runnable) args[0]);
                yield proxy;
            }
            case "streamFilter" -> {
                harness.responseStreamFilter().set(compose(harness.responseStreamFilter().get(), streamFilter(args)));
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });

        if (harness.endpointContext() != null) {
            context.register(harness.endpointContext());
            harness.endpointContext().markResourceStart();
            harness.endpointContext().markResourceEnd(false);
        }
        if (skipResponseStatusMetrics) {
            context.register(OciHttpEndpointMetricsContext.SKIP_RESPONSE_STATUS_METRICS, true);
        }
        if (runtimeDimensionValue != null) {
            context.register(RUNTIME_PROPERTY, runtimeDimensionValue);
        }

        harness.inFlightWork().set(() -> {
            if (readRequestBody && harness.requestStreamFilter().get() != null) {
                readRequestBody(harness);
            }
            if (responseBodyStream != null && harness.responseStreamFilter().get() != null) {
                writeResponseBody(harness, responseBodyStream, closeResponseBodyStream);
            }
        });
        harness.filter().filter(harness.chain(), request, response);
    }

    private static ServerRequestHeaders headers(String userAgent) {
        if (userAgent == null) {
            return ServerRequestHeaders.create();
        }
        return ServerRequestHeaders.create(WritableHeaders.create()
                                                   .add(HeaderValues.create(HeaderNames.USER_AGENT, userAgent)));
    }

    private static void readRequestBody(MetricsTestHarness harness) {
        UnaryOperator<InputStream> filter = harness.requestStreamFilter().get();
        assertThat(filter, notNullValue());
        try (InputStream inputStream = filter.apply(new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)))) {
            while (inputStream.read() != -1) {
                // read until EOF
            }
        } catch (Exception e) {
            fail(e);
        }
    }

    private static void writeResponseBody(MetricsTestHarness harness, OutputStream target, boolean closeResponseBodyStream) {
        UnaryOperator<OutputStream> filter = harness.responseStreamFilter().get();
        assertThat(filter, notNullValue());
        OutputStream outputStream = filter.apply(target);
        try {
            outputStream.write("body".getBytes(StandardCharsets.UTF_8));
            if (closeResponseBodyStream) {
                outputStream.close();
            }
        } catch (Exception e) {
            fail(e);
        }
    }

    private static <T> UnaryOperator<T> compose(UnaryOperator<T> current, UnaryOperator<T> next) {
        return current == null ? next : stream -> next.apply(current.apply(stream));
    }

    @SuppressWarnings("unchecked")
    private static <T> UnaryOperator<T> streamFilter(Object[] args) {
        return (UnaryOperator<T>) args[0];
    }

    private static void assertCoreMeters(MetricsTestHarness harness,
                                         String scope,
                                         int statusCode,
                                         String statusFamily,
                                         double successRate,
                                         boolean hasResourceTime) {
        assertMetricPresent(harness, scope + ".Time");
        if (hasResourceTime) {
            assertMetricPresent(harness, scope + ".ResourceTime");
        }
        assertMetricValue(harness, scope + ".ResponseOut.StatusCode." + statusCode + ".Count", 1.0);
        assertMetricValue(harness, scope + ".ResponseOut.StatusFamily." + statusFamily + ".Count", 1.0);
        assertMetricValue(harness, scope + ".ResponseOut.Count", 1.0);
        assertMetricValue(harness, scope + ".SuccessRate", successRate);
    }

    private static void assertMetricValue(MetricsTestHarness harness, String name, double value) {
        TimeSeries series = metric(harness, name);
        assertTrue(series.getObservations()
                           .stream()
                           .anyMatch(observation -> Double.compare(observation.getValue(), value) == 0),
                   () -> "Expected metric "
                           + name
                           + " to contain value "
                           + value
                           + " in observations "
                           + series.getObservations());
    }

    private static void assertMetricObservationCount(MetricsTestHarness harness, String name, long count) {
        TimeSeries series = metric(harness, name);
        assertThat(series.getObservations().stream().mapToLong(observation -> observation.getCount()).sum(), is(count));
    }

    private static void assertMetricDimensions(MetricsTestHarness harness, String name, Map<String, String> dimensions) {
        TimeSeries series = metric(harness, name, dimensions);
        if (dimensions.isEmpty()) {
            assertFalse(series.getMetricName().getDimensions().containsKey(RUNTIME_DIMENSION));
            return;
        }
        dimensions.forEach((key, value) -> assertThat(series.getMetricName().getDimensions().get(key), is(value)));
    }

    private static void assertMetricPresent(MetricsTestHarness harness, String name) {
        metric(harness, name);
    }

    private static void assertMetricAbsent(MetricsTestHarness harness, String name) {
        List<TimeSeries> flushedMetrics = harness.flushMetrics();
        assertFalse(hasMetric(flushedMetrics, name));
    }

    private static boolean hasMetric(List<TimeSeries> flushedMetrics, String name) {
        return flushedMetrics
                .stream()
                .anyMatch(series -> series.getMetricName().getName().equals(name));
    }

    private static TimeSeries metric(MetricsTestHarness harness, String name) {
        List<TimeSeries> flushedMetrics = harness.flushMetrics();
        return flushedMetrics
                .stream()
                .filter(series -> series.getMetricName().getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected metric " + name + " in " + flushedMetrics));
    }

    private static TimeSeries metric(MetricsTestHarness harness, String name, Map<String, String> dimensions) {
        List<TimeSeries> flushedMetrics = harness.flushMetrics();
        return flushedMetrics
                .stream()
                .filter(series -> series.getMetricName().getName().equals(name))
                .filter(series -> series.getMetricName().getDimensions().entrySet().containsAll(dimensions.entrySet()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected metric "
                                                              + name
                                                              + " with dimensions "
                                                              + dimensions
                                                              + " in "
                                                              + flushedMetrics));
    }

    private static void assertRequestProceeded(MetricsTestHarness harness) {
        assertThat(harness.proceedCount().get(), is(1));
    }

    private static OciAutoHttpMetricsConfig autoHttpRuntimeDimension() {
        return autoHttpRuntimeDimension(null);
    }

    private static OciAutoHttpMetricsConfig autoHttpRuntimeDimension(String defaultDimension) {
        OciAutoHttpRuntimeDimensionConfig.Builder runtimeDimension = OciAutoHttpRuntimeDimensionConfig.builder()
                .propertyName(RUNTIME_PROPERTY)
                .dimensionName(RUNTIME_DIMENSION);
        if (defaultDimension != null) {
            runtimeDimension.defaultDimension(defaultDimension);
        }
        return OciAutoHttpMetricsConfig.builder()
                .runtimeDimension(runtimeDimension.build())
                .build();
    }

    private record MetricsTestHarness(Filter filter,
                                      OciMeterRegistry registry,
                                      AtomicInteger addCount,
                                      AtomicInteger proceedCount,
                                      FilterChain chain,
                                      AtomicReference<Runnable> whenSentHandler,
                                      AtomicReference<Runnable> inFlightWork,
                                      AtomicReference<UnaryOperator<InputStream>> requestStreamFilter,
                                      AtomicReference<UnaryOperator<OutputStream>> responseStreamFilter,
                                      CapturingMetricReporter reporter,
                                      OciMetricsPublisher publisher,
                                      OciHttpEndpointMetricsContext endpointContext,
                                      AtomicReference<List<TimeSeries>> flushedMetrics) {

        private static MetricsTestHarness create(OciHttpEndpointMetricsContext endpointContext) {
            return create(endpointContext, true);
        }

        private static MetricsTestHarness create(OciHttpEndpointMetricsContext endpointContext,
                                                 boolean detailedTimingEnabled) {
            return create(endpointContext, detailedTimingEnabled, Optional.empty());
        }

        private static MetricsTestHarness create(OciHttpEndpointMetricsContext endpointContext,
                                                 boolean detailedTimingEnabled,
                                                 Optional<String> resourcePackagePrefix) {
            return create(endpointContext, detailedTimingEnabled, resourcePackagePrefix, builder -> {
            });
        }

        private static MetricsTestHarness create(OciHttpEndpointMetricsContext endpointContext,
                                                 boolean detailedTimingEnabled,
                                                 Optional<String> resourcePackagePrefix,
                                                 Consumer<OciMetricsPublisherConfig.Builder> publisherConfigCustomizer) {
            return create(endpointContext,
                          detailedTimingEnabled,
                          resourcePackagePrefix,
                          publisherConfigCustomizer,
                          builder -> {
                          });
        }

        private static MetricsTestHarness createWithMetricsConfig(OciHttpEndpointMetricsContext endpointContext,
                                                                  Consumer<MetricsConfig.Builder> metricsConfigCustomizer) {
            return create(endpointContext,
                          true,
                          Optional.empty(),
                          builder -> {
                          },
                          metricsConfigCustomizer);
        }

        private static MetricsTestHarness create(OciHttpEndpointMetricsContext endpointContext,
                                                 boolean detailedTimingEnabled,
                                                 Optional<String> resourcePackagePrefix,
                                                 Consumer<OciMetricsPublisherConfig.Builder> publisherConfigCustomizer,
                                                 Consumer<MetricsConfig.Builder> metricsConfigCustomizer) {
            AtomicInteger addCount = new AtomicInteger();
            MetricsConfig metricsConfig = metricsConfig(metricsConfigCustomizer);
            CapturingMetricReporter reporter = new CapturingMetricReporter();
            OciMetricsPublisherConfig.Builder publisherConfigBuilder = OciMetricsPublisherConfig.builder()
                    .enabled(true)
                    .enableDetailedTimingAutoMetrics(detailedTimingEnabled)
                    .reporterConfig(OverlayMetricReporterConfig.builder()
                                            .project("proj")
                                            .fleet("fleet")
                                            .build())
                    .reporter(reporter)
                    .defaultDimensions(Map.of());
            resourcePackagePrefix.ifPresent(publisherConfigBuilder::resourcePackagePrefix);
            publisherConfigCustomizer.accept(publisherConfigBuilder);
            OciMetricsPublisherConfig publisherConfig = publisherConfigBuilder.buildPrototype();
            MetricsFactory delegateFactory = delegateFactory(metricsConfig);
            OciMetricsPublisher publisher = OciMetricsPublisher.create(publisherConfig);
            OciMeterRegistry registry = new OciMeterRegistry(metricsConfig,
                                                             Clock.system(),
                                                             meter -> addCount.incrementAndGet(),
                                                             meter -> {
                                                             },
                                                             publisher,
                                                             delegateFactory,
                                                             delegateFactory.createMeterRegistry(Clock.system(),
                                                                                                 metricsConfig));

            return create(endpointContext, addCount, metricsConfig, registry, reporter, publisher);
        }

        private static boolean filterPresent(Consumer<OciMetricsPublisherConfig.Builder> publisherConfigCustomizer) {
            AtomicInteger addCount = new AtomicInteger();
            MetricsConfig metricsConfig = metricsConfig();
            CapturingMetricReporter reporter = new CapturingMetricReporter();
            OciMetricsPublisherConfig.Builder publisherConfigBuilder = OciMetricsPublisherConfig.builder()
                    .enabled(true)
                    .reporter(reporter)
                    .defaultDimensions(Map.of());
            publisherConfigCustomizer.accept(publisherConfigBuilder);
            OciMetricsPublisherConfig publisherConfig = publisherConfigBuilder.buildPrototype();
            MetricsFactory delegateFactory = delegateFactory(metricsConfig);
            OciMeterRegistry registry = new OciMeterRegistry(metricsConfig,
                                                             Clock.system(),
                                                             meter -> addCount.incrementAndGet(),
                                                             meter -> {
                                                             },
                                                             OciMetricsPublisher.create(publisherConfig),
                                                             delegateFactory,
                                                             delegateFactory.createMeterRegistry(Clock.system(),
                                                                                                 metricsConfig));
            MetricsObserverConfig config = MetricsObserverConfig.builder()
                    .metricsConfig(metricsConfig)
                    .meterRegistry(registry)
                    .buildPrototype();
            return new OciMetricsSemanticConventions(registry).filter(config).isPresent();
        }

        private static MetricsTestHarness createWithNullPublisherPrototype(OciHttpEndpointMetricsContext endpointContext) {
            AtomicInteger addCount = new AtomicInteger();
            MetricsConfig metricsConfig = metricsConfig();
            MetricsFactory delegateFactory = delegateFactory(metricsConfig);
            CapturingMetricReporter reporter = new CapturingMetricReporter();
            OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.builder()
                    .enabled(true)
                    .reporter(reporter)
                    .defaultDimensions(Map.of())
                    .buildPrototype();
            OciMetricsPublisher publisher = OciMetricsPublisher.create((OciMetricsPublisherConfig) null);
            OciMeterRegistry registry = new OciMeterRegistry(metricsConfig,
                                                             Clock.system(),
                                                             meter -> addCount.incrementAndGet(),
                                                             meter -> {
                                                             },
                                                             publisher,
                                                             delegateFactory,
                                                             delegateFactory.createMeterRegistry(Clock.system(),
                                                                                                 metricsConfig));
            return create(endpointContext, addCount, metricsConfig, registry, reporter, publisher);
        }

        private static MetricsTestHarness create(OciHttpEndpointMetricsContext endpointContext,
                                                 AtomicInteger addCount,
                                                 MetricsConfig metricsConfig,
                                                 OciMeterRegistry registry,
                                                 CapturingMetricReporter reporter,
                                                 OciMetricsPublisher publisher) {
            MetricsObserverConfig config = MetricsObserverConfig.builder()
                    .metricsConfig(metricsConfig)
                    .meterRegistry(registry)
                    .buildPrototype();

            Filter filter = new OciMetricsSemanticConventions(registry).filter(config).orElseThrow();
            AtomicReference<Runnable> whenSentHandler = new AtomicReference<>();
            AtomicReference<Runnable> inFlightWork = new AtomicReference<>(() -> {
            });
            AtomicReference<UnaryOperator<InputStream>> requestStreamFilter = new AtomicReference<>();
            AtomicReference<UnaryOperator<OutputStream>> responseStreamFilter = new AtomicReference<>();
            AtomicInteger proceedCount = new AtomicInteger();
            FilterChain chain = proxy(FilterChain.class, proceed(whenSentHandler, inFlightWork, proceedCount));

            return new MetricsTestHarness(filter,
                                          registry,
                                          addCount,
                                          proceedCount,
                                          chain,
                                          whenSentHandler,
                                          inFlightWork,
                                          requestStreamFilter,
                                          responseStreamFilter,
                                          reporter,
                                          publisher,
                                          endpointContext,
                                          new AtomicReference<>());
        }

        private List<TimeSeries> flushMetrics() {
            List<TimeSeries> currentFlush = flushedMetrics.get();
            if (currentFlush != null) {
                return currentFlush;
            }
            awaitAsyncMetrics();
            reporter.clear();
            if (!Metrics.isActive()) {
                Metrics.init(reporter, Map.of());
            }
            registry.counters().stream()
                    .filter(AbstractOciMeter::enabled)
                    .forEach(counter -> publisher.publishCounterDelta(counter, counter.drainDelta()));
            registry.timers().stream()
                    .filter(AbstractOciMeter::enabled)
                    .forEach(timer -> publisher.publishObservations(timer,
                                                                    timer.drainAllIntervalSamples(),
                                                                    "timer duration samples"));
            registry.distributionSummaries().stream()
                    .filter(AbstractOciMeter::enabled)
                    .forEach(summary -> publisher.publishObservations(summary,
                                                                      summary.drainAllIntervalSamples(),
                                                                      "distribution summary samples"));
            publisher.publishAccumulatorPressure(0L);
            Metrics.shutdown();
            List<TimeSeries> result = reporter.drain();
            flushedMetrics.set(result);
            return result;
        }

        private void awaitAsyncMetrics() {
            if (!OciMetricsSemanticConventions.awaitAsyncUpdates(Duration.ofSeconds(30))) {
                fail("Timed out waiting for asynchronous HTTP metrics update");
            }
        }

        private void invalidateFlush() {
            flushedMetrics.set(null);
        }
    }

    private static final class CapturingMetricReporter implements MetricReporter {
        private final List<TimeSeries> timeSeries = new CopyOnWriteArrayList<>();

        @Override
        public void send(List<TimeSeries> timeSeries) {
            this.timeSeries.addAll(timeSeries);
        }

        private List<TimeSeries> drain() {
            List<TimeSeries> result = List.copyOf(timeSeries);
            timeSeries.clear();
            return result;
        }

        private void clear() {
            timeSeries.clear();
        }
    }

    private static InvocationHandler proceed(AtomicReference<Runnable> whenSentHandler,
                                             AtomicReference<Runnable> inFlightWork,
                                             AtomicInteger proceedCount) {
        return (proxy, method, args) -> {
            if ("proceed".equals(method.getName())) {
                proceedCount.incrementAndGet();
                inFlightWork.get().run();
                Runnable runnable = whenSentHandler.get();
                if (runnable != null) {
                    runnable.run();
                }
            }
            return null;
        };
    }

    private static MetricsConfig metricsConfig() {
        return metricsConfig(builder -> {
        });
    }

    private static MetricsConfig metricsConfig(Consumer<MetricsConfig.Builder> customizer) {
        MetricsConfig.Builder builder = MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false);
        customizer.accept(builder);
        return builder.build();
    }

    private static MetricsFactory delegateFactory(MetricsConfig metricsConfig) {
        return new MicrometerMetricsFactoryProvider().create(Config.empty(), metricsConfig, java.util.List.of());
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
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

    private static final class CountingOutputStream extends OutputStream {
        private int singleByteWriteCount;
        private int bulkWriteCount;

        @Override
        public void write(int value) {
            singleByteWriteCount++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            bulkWriteCount++;
        }

        int singleByteWriteCount() {
            return singleByteWriteCount;
        }

        int bulkWriteCount() {
            return bulkWriteCount;
        }
    }
}
