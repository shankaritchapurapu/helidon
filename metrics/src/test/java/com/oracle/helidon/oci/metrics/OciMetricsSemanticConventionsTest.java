/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.uri.UriPath;
import io.helidon.config.Config;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Method;
import io.helidon.http.PathMatchers;
import io.helidon.http.RoutedPath;
import io.helidon.http.Status;
import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Counter;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class OciMetricsSemanticConventionsTest {

    @Test
    void reusesHttpMetersForRepeatedRequestsWithSameSemanticKey() {
        MetricsTestHarness harness = MetricsTestHarness.create();

        recordRequest(harness, "/greet/tim", Optional.of("/greet/{name}"), Status.OK_200);
        recordRequest(harness, "/greet/tim", Optional.of("/greet/{name}"), Status.OK_200);

        assertThat(harness.addCount().get(), is(2));
        assertHttpMeters(harness, "/greet/{name}", "2xx", 2L);
    }

    @Test
    void unmatchedNotFoundRequestsUseEmptyRoute() {
        MetricsTestHarness harness = MetricsTestHarness.create();

        recordRequest(harness, "/bad", Optional.empty(), Status.NOT_FOUND_404);
        recordRequest(harness, "/otherBad", Optional.empty(), Status.NOT_FOUND_404);

        assertThat(harness.addCount().get(), is(2));
        assertHttpMeters(harness, "", "4xx", 2L);
    }

    @Test
    void matchedClientErrorPreservesMatchedRoute() {
        MetricsTestHarness harness = MetricsTestHarness.create();

        recordRequest(harness, "/secure/123", Optional.of("/secure/{id}"), Status.FORBIDDEN_403);

        assertThat(harness.addCount().get(), is(2));
        assertHttpMeters(harness, "/secure/{id}", "4xx", 1L);
    }

    @Test
    void unmatchedNonNotFoundClientErrorUsesConcretePath() {
        MetricsTestHarness harness = MetricsTestHarness.create();

        recordRequest(harness, "/somepath", Optional.empty(), Status.UNAUTHORIZED_401);

        assertThat(harness.addCount().get(), is(2));
        assertHttpMeters(harness, "/somepath", "4xx", 1L);
    }

    @Test
    void successfulFixedPathUsesConcretePath() {
        MetricsTestHarness harness = MetricsTestHarness.create();

        recordRequest(harness, "/greet", Optional.empty(), Status.OK_200);

        assertThat(harness.addCount().get(), is(2));
        assertHttpMeters(harness, "/greet", "2xx", 1L);
    }

    private static void recordRequest(MetricsTestHarness harness,
                                      String path,
                                      Optional<String> matchingPattern,
                                      Status status) {
        RoutingRequest request = proxy(RoutingRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "prologue" -> HttpPrologue.create("HTTP/1.1", "HTTP", "1.1", Method.GET, path, false);
            case "matchingPattern" -> args == null ? matchingPattern : proxy;
            /*
            The handler receives both path() and path(RoutedPath). Because this switch matches only on method name,
            return a RoutedPath for path() and the request proxy for the fluent path(RoutedPath) overload.
             */
            case "path" -> args == null ? routedPath(path) : proxy;
            default -> defaultValue(method.getReturnType());
        });

        RoutingResponse response = proxy(RoutingResponse.class, (proxy, method, args) -> switch (method.getName()) {
            case "status" -> status;
            case "whenSent" -> {
                harness.whenSentHandler().set((Runnable) args[0]);
                yield proxy;
            }
            default -> defaultValue(method.getReturnType());
        });

        harness.filter().filter(harness.chain(), request, response);
    }

    private static void assertHttpMeters(MetricsTestHarness harness, String route, String statusFamily, long count) {
        Iterable<Tag> tags = List.of(Tag.create("method", "GET"),
                                     Tag.create("route", route),
                                     Tag.create("status.family", statusFamily));
        Counter counter = harness.registry().meter(Counter.class, "http.requests.count", tags).orElseThrow();
        Timer timer = harness.registry().meter(Timer.class, "http.request.duration", tags).orElseThrow();

        assertThat(counter.count(), is(count));
        assertThat(timer.count(), is(count));
    }

    private record MetricsTestHarness(Filter filter,
                                      OciMeterRegistry registry,
                                      AtomicInteger addCount,
                                      FilterChain chain,
                                      AtomicReference<Runnable> whenSentHandler) {

        private static MetricsTestHarness create() {
            AtomicInteger addCount = new AtomicInteger();
            MetricsConfig metricsConfig = metricsConfig();
            OciMetricsFactory factory = new OciMetricsFactory(delegateFactory(metricsConfig),
                                                              OciMetricsPublisherConfig.builder()
                                                                      .enabled(false)
                                                                      .project("proj")
                                                                      .fleet("fleet")
                                                                      .defaultDimensions(Map.of())
                                                                      .requestHeaders(Map.of())
                                                                      .buildPrototype(),
                                                              metricsConfig,
                                                              java.util.List.of());
            OciMeterRegistry registry = (OciMeterRegistry) factory.createMeterRegistry(Clock.system(),
                                                                                       metricsConfig,
                                                                                       meter -> addCount.incrementAndGet(),
                                                                                       meter -> {
                                                                                       });

            MetricsObserverConfig config = MetricsObserverConfig.builder()
                    .metricsConfig(metricsConfig)
                    .meterRegistry(registry)
                    .buildPrototype();

            Filter filter = new OciMetricsSemanticConventions(registry).filter(config).orElseThrow();
            AtomicReference<Runnable> whenSentHandler = new AtomicReference<>();
            FilterChain chain = proxy(FilterChain.class, (proxy, method, args) -> {
                Runnable runnable = whenSentHandler.get();
                assertThat(runnable, notNullValue());
                runnable.run();
                return null;
            });

            return new MetricsTestHarness(filter, registry, addCount, chain, whenSentHandler);
        }
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

    private static RoutedPath routedPath(String path) {
        return PathMatchers.exact(path)
                .match(UriPath.create(path))
                .path();
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
