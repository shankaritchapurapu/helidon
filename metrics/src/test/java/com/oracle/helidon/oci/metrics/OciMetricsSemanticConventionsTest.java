/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.config.Config;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Method;
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
        RoutingRequest request = proxy(RoutingRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "prologue" -> HttpPrologue.create("HTTP/1.1", "HTTP", "1.1", Method.GET, "/greet/tim", false);
            case "matchingPattern" -> Optional.of("/greet/{name}");
            default -> defaultValue(method.getReturnType());
        });
        AtomicReference<Runnable> whenSentHandler = new AtomicReference<>();
        RoutingResponse response = proxy(RoutingResponse.class, (proxy, method, args) -> switch (method.getName()) {
            case "status" -> Status.OK_200;
            case "whenSent" -> {
                whenSentHandler.set((Runnable) args[0]);
                yield proxy;
            }
            default -> defaultValue(method.getReturnType());
        });
        FilterChain chain = proxy(FilterChain.class, (proxy, method, args) -> {
            Runnable runnable = whenSentHandler.get();
            assertThat(runnable, notNullValue());
            runnable.run();
            return null;
        });

        filter.filter(chain, request, response);
        filter.filter(chain, request, response);

        Iterable<Tag> tags = java.util.List.of(Tag.create("method", "GET"),
                                               Tag.create("route", "/greet/{name}"),
                                               Tag.create("status.family", "2xx"));
        Counter counter = registry.meter(Counter.class, "http.requests.count", tags).orElseThrow();
        Timer timer = registry.meter(Timer.class, "http.request.duration", tags).orElseThrow();

        assertThat(addCount.get(), is(2));
        assertThat(counter.count(), is(2L));
        assertThat(timer.count(), is(2L));
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
}
