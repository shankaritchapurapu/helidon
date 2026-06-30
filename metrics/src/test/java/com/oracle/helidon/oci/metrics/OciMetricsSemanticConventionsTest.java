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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import io.helidon.common.context.Context;
import io.helidon.config.Config;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Method;
import io.helidon.http.Status;
import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.api.Timer;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.helidon.webserver.observe.metrics.MetricsObserverConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OciMetricsSemanticConventionsTest {
    private static final String MATCHING_RESOURCE_CLASS = "com.example.store.StoreEndpoint";
    private static final String OTHER_RESOURCE_CLASS = "com.example.other.OtherEndpoint";

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
        assertTrue(harness.registry()
                           .meter(Timer.class, "UnknownMethod.ResourceTime", List.<Tag>of())
                           .isEmpty());
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

        assertTimerCount(harness, "StoreEndpoint.post.WireReadTime", 1L);
        assertTimerAbsent(harness, "StoreEndpoint.post.WireWriteTime");
    }

    @Test
    void noRequestBodyReadEmitsNoWireReadTime() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));

        recordRequest(harness, "/store/items/one", Status.OK_200, false, false);

        assertTimerAbsent(harness, "StoreEndpoint.get.WireReadTime");
    }

    @Test
    void responseBodyWriteEmitsWireWriteTime() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));

        recordRequest(harness, "/store/items/one", Status.OK_200, false, true);

        assertTimerCount(harness, "StoreEndpoint.get.WireWriteTime", 1L);
        assertTimerAbsent(harness, "StoreEndpoint.get.WireReadTime");
    }

    @Test
    void responseBodyWriteEmitsWireWriteTimeOnFrameworkManagedCompletion() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.get"));

        recordRequest(harness, "/store/items/one", Status.OK_200, false, new ByteArrayOutputStream(), false);

        assertTimerCount(harness, "StoreEndpoint.get.WireWriteTime", 1L);
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
        assertTimerCount(harness, "StoreEndpoint.get.WireWriteTime", 1L);
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

        assertTimerAbsent(harness, "StoreEndpoint.delete.WireWriteTime");
    }

    @Test
    void wireTimersUseSecondaryScopes() {
        MetricsTestHarness harness = MetricsTestHarness.create(
                new OciHttpEndpointMetricsContext("StoreEndpoint.put",
                                                  List.of("StoreApi", "WriteApi"),
                                                  MATCHING_RESOURCE_CLASS));

        recordRequest(harness, "/store/items/one", Status.OK_200, true, true);

        assertTimerCount(harness, "StoreEndpoint.put.WireReadTime", 1L);
        assertTimerCount(harness, "StoreEndpoint.put.WireWriteTime", 1L);
        assertTimerCount(harness, "StoreApi.WireReadTime", 1L);
        assertTimerCount(harness, "StoreApi.WireWriteTime", 1L);
        assertTimerCount(harness, "WriteApi.WireReadTime", 1L);
        assertTimerCount(harness, "WriteApi.WireWriteTime", 1L);
    }

    @Test
    void wireTimersFallBackToUnknownMethodWithoutGeneratedEndpointContext() {
        MetricsTestHarness harness = MetricsTestHarness.create(null);

        recordRequest(harness, "/unknown", Status.NOT_FOUND_404, true, true);

        assertTimerCount(harness, "UnknownMethod.WireReadTime", 1L);
        assertTimerCount(harness, "UnknownMethod.WireWriteTime", 1L);
    }

    @Test
    void disabledDetailedTimingSuppressesTimingOnly() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.disabledTiming"), false);

        recordRequest(harness, "/store/items", Status.OK_200, true, true);

        assertTimerAbsent(harness, "StoreEndpoint.disabledTiming.Time");
        assertTimerAbsent(harness, "StoreEndpoint.disabledTiming.ResourceTime");
        assertTimerAbsent(harness, "StoreEndpoint.disabledTiming.WireReadTime");
        assertTimerAbsent(harness, "StoreEndpoint.disabledTiming.WireWriteTime");
        assertCounterCount(harness, "StoreEndpoint.disabledTiming.ResponseOut.StatusCode.200.Count", 1L);
        assertCounterCount(harness, "StoreEndpoint.disabledTiming.ResponseOut.StatusFamily.2XX.Count", 1L);
        assertCounterCount(harness, "StoreEndpoint.disabledTiming.ResponseOut.Count", 1L);
        DistributionSummary summary = harness.registry()
                .meter(DistributionSummary.class, "StoreEndpoint.disabledTiming.SuccessRate", List.<Tag>of())
                .orElseThrow();
        assertThat(summary.count(), is(1L));
        assertThat(summary.totalAmount(), is(1.0));
    }

    @Test
    void nullPublisherPrototypeFallsBackToDefaultAutoMetricsConfig() {
        MetricsTestHarness harness = MetricsTestHarness.createWithNullPublisherPrototype(endpoint("StoreEndpoint.nullPrototype"));

        recordRequest(harness, "/store/items", Status.OK_200, true, true);

        assertCoreMeters(harness, "StoreEndpoint.nullPrototype", 200, "2XX", 1.0, true);
        assertTimerCount(harness, "StoreEndpoint.nullPrototype.WireReadTime", 1L);
        assertTimerCount(harness, "StoreEndpoint.nullPrototype.WireWriteTime", 1L);
    }

    @Test
    void resourcePackagePrefixAllowsMatchingResource() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.filtered"),
                                                               true,
                                                               Optional.of("com.example.store"));

        recordRequest(harness, "/store/items", Status.OK_200, true, true);

        assertCoreMeters(harness, "StoreEndpoint.filtered", 200, "2XX", 1.0, true);
        assertTimerCount(harness, "StoreEndpoint.filtered.WireReadTime", 1L);
        assertTimerCount(harness, "StoreEndpoint.filtered.WireWriteTime", 1L);
    }

    @Test
    void resourcePackagePrefixSuppressesNonMatchingResource() {
        MetricsTestHarness harness = MetricsTestHarness.create(endpoint("StoreEndpoint.filteredOut", OTHER_RESOURCE_CLASS),
                                                               true,
                                                               Optional.of("com.example.store"));

        recordRequest(harness, "/other/items", Status.OK_200, true, true);

        assertRequestProceeded(harness);
        assertThat(harness.addCount().get(), is(0));
        assertTimerAbsent(harness, "StoreEndpoint.filteredOut.Time");
        assertTimerAbsent(harness, "StoreEndpoint.filteredOut.WireReadTime");
        assertTimerAbsent(harness, "StoreEndpoint.filteredOut.WireWriteTime");
    }

    @Test
    void resourcePackagePrefixSuppressesUnknownMethod() {
        MetricsTestHarness harness = MetricsTestHarness.create(null, true, Optional.of("com.example.store"));

        recordRequest(harness, "/unknown", Status.NOT_FOUND_404, true, true);

        assertRequestProceeded(harness);
        assertThat(harness.addCount().get(), is(0));
        assertTimerAbsent(harness, "UnknownMethod.Time");
        assertTimerAbsent(harness, "UnknownMethod.WireReadTime");
        assertTimerAbsent(harness, "UnknownMethod.WireWriteTime");
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
        Context context = Context.create();
        RoutingRequest request = proxy(RoutingRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "prologue" -> HttpPrologue.create("HTTP/1.1", "HTTP", "1.1", Method.GET, path, false);
            case "context" -> context;
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
        assertTimerCount(harness, scope + ".Time", 1L);
        if (hasResourceTime) {
            assertTimerCount(harness, scope + ".ResourceTime", 1L);
        }
        assertCounterCount(harness, scope + ".ResponseOut.StatusCode." + statusCode + ".Count", 1L);
        assertCounterCount(harness, scope + ".ResponseOut.StatusFamily." + statusFamily + ".Count", 1L);
        assertCounterCount(harness, scope + ".ResponseOut.Count", 1L);

        DistributionSummary summary = harness.registry()
                .meter(DistributionSummary.class, scope + ".SuccessRate", List.<Tag>of())
                .orElseThrow();
        assertThat(summary.count(), greaterThanOrEqualTo(1L));
        assertThat(summary.totalAmount(), greaterThanOrEqualTo(successRate));
        assertThat(((OciDistributionSummary) summary).intervalMeanIfChanged(), is(OptionalDouble.of(successRate)));
    }

    private static void assertCounterCount(MetricsTestHarness harness, String name, long count) {
        Counter counter = harness.registry().meter(Counter.class, name, List.<Tag>of()).orElseThrow();
        assertThat(counter.count(), greaterThanOrEqualTo(count));
    }

    private static void assertTimerCount(MetricsTestHarness harness, String name, long count) {
        Timer timer = harness.registry().meter(Timer.class, name, List.<Tag>of()).orElseThrow();
        assertThat(timer.count(), greaterThanOrEqualTo(count));
    }

    private static void assertTimerAbsent(MetricsTestHarness harness, String name) {
        assertTrue(harness.registry()
                           .meter(Timer.class, name, List.<Tag>of())
                           .isEmpty());
    }

    private static void assertRequestProceeded(MetricsTestHarness harness) {
        assertThat(harness.proceedCount().get(), is(1));
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
                                      OciHttpEndpointMetricsContext endpointContext) {

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
            AtomicInteger addCount = new AtomicInteger();
            MetricsConfig metricsConfig = metricsConfig();
            OciMetricsPublisherConfig.Builder publisherConfigBuilder = OciMetricsPublisherConfig.builder()
                    .enabled(false)
                    .enableDetailedTimingAutoMetrics(detailedTimingEnabled)
                    .project("proj")
                    .fleet("fleet")
                    .defaultDimensions(Map.of())
                    .requestHeaders(Map.of());
            resourcePackagePrefix.ifPresent(publisherConfigBuilder::resourcePackagePrefix);
            OciMetricsFactory factory = new OciMetricsFactory(delegateFactory(metricsConfig),
                                                              publisherConfigBuilder.buildPrototype(),
                                                              metricsConfig,
                                                              java.util.List.of());
            OciMeterRegistry registry = (OciMeterRegistry) factory.createMeterRegistry(Clock.system(),
                                                                                       metricsConfig,
                                                                                       meter -> addCount.incrementAndGet(),
                                                                                       meter -> {
                                                                                       });

            return create(endpointContext, addCount, metricsConfig, registry);
        }

        private static MetricsTestHarness createWithNullPublisherPrototype(OciHttpEndpointMetricsContext endpointContext) {
            AtomicInteger addCount = new AtomicInteger();
            MetricsConfig metricsConfig = metricsConfig();
            MetricsFactory delegateFactory = delegateFactory(metricsConfig);
            OciMeterRegistry registry = new OciMeterRegistry(metricsConfig,
                                                             Clock.system(),
                                                             meter -> addCount.incrementAndGet(),
                                                             meter -> {
                                                             },
                                                             OciMetricsPublisher.create((OciMetricsPublisherConfig) null),
                                                             delegateFactory,
                                                             delegateFactory.createMeterRegistry(Clock.system(),
                                                                                                 metricsConfig));
            return create(endpointContext, addCount, metricsConfig, registry);
        }

        private static MetricsTestHarness create(OciHttpEndpointMetricsContext endpointContext,
                                                 AtomicInteger addCount,
                                                 MetricsConfig metricsConfig,
                                                 OciMeterRegistry registry) {
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
                                          endpointContext);
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
        return MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .build();
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
