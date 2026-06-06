/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import io.helidon.http.Status;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.api.Timer;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.helidon.webserver.observe.metrics.MetricsObserverConfig;
import io.helidon.webserver.observe.metrics.spi.AutoHttpMetricsProvider;

/**
 * HTTP semantic-convention metrics for the OCI metrics provider: a count of requests and a timer accumulating time spent
 * processing requests.
 */
@Service.Singleton
class OciMetricsSemanticConventions implements AutoHttpMetricsProvider {

    private static final String HTTP_REQUEST_COUNT = "http.requests.count";
    private static final String HTTP_REQUEST_DURATION = "http.request.duration";

    private final OciMeterRegistry ociMeterRegistry;

    @Service.Inject
    OciMetricsSemanticConventions(OciMeterRegistry ociMeterRegistry) {
        this.ociMeterRegistry = ociMeterRegistry;
    }

    @Override
    public Optional<Filter> filter(MetricsObserverConfig config) {
        if (!config.enabled()) {
            return Optional.empty();
        }

        /*
        Unless explicitly disabled, collect the automatic metrics using the filter.
         */
        return (config.autoHttpMetrics().isPresent() && !config.autoHttpMetrics().get().enabled())
                ? Optional.empty()
                : Optional.of(new OciHttpMetricsFilter(ociMeterRegistry));
    }

    private static final class OciHttpMetricsFilter implements Filter {

        private final OciMeterRegistry registry;
        private final ConcurrentMap<HttpRequestMetricKey, HttpRequestMeters> metersByKey = new ConcurrentHashMap<>();

        private OciHttpMetricsFilter(OciMeterRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void filter(FilterChain chain, RoutingRequest request, RoutingResponse response) {
            long startNanos = System.nanoTime();
            response.whenSent(() -> updateMetrics(request, response, System.nanoTime() - startNanos));
            chain.proceed();
        }

        private void updateMetrics(RoutingRequest request, RoutingResponse response, long elapsedNanos) {
            HttpRequestMetricKey key = new HttpRequestMetricKey(request.prologue().method().text(),
                                                                route(request, response),
                                                                (response.status().code() / 100) + "xx");
            HttpRequestMeters meters = metersByKey.computeIfAbsent(key, this::createMeters);
            meters.counter().increment();
            meters.timer().record(elapsedNanos, TimeUnit.NANOSECONDS);
        }

        private HttpRequestMeters createMeters(HttpRequestMetricKey key) {
            Counter counter = registry.getOrCreate(OciCounter.builder(HTTP_REQUEST_COUNT)
                                                           .addTag(Tag.create("method", key.method()))
                                                           .addTag(Tag.create("route", key.route()))
                                                           .addTag(Tag.create("status.family", key.statusFamily())));
            Timer timer = registry.getOrCreate(OciTimer.builder(HTTP_REQUEST_DURATION)
                                                       .addTag(Tag.create("method", key.method()))
                                                       .addTag(Tag.create("route", key.route()))
                                                       .addTag(Tag.create("status.family", key.statusFamily())));
            return new HttpRequestMeters(counter, timer);
        }

        private String route(RoutingRequest request, RoutingResponse response) {
            return request.matchingPattern()
                    .orElseGet(() -> routeNotFound(response.status())
                            ? ""
                            : request.path().path());
        }

        private boolean routeNotFound(Status status) {
            /*
            Helidon routing reports no matched route as 404. Other no-pattern responses can still come from legitimate
            fixed-path routes or filters, so keep their concrete path instead of treating every unmatched status as
            high-cardinality.
             */
            return status.code() == Status.NOT_FOUND_404.code();
        }
    }

    private record HttpRequestMetricKey(String method, String route, String statusFamily) {
    }

    private record HttpRequestMeters(Counter counter, Timer timer) {
    }
}
