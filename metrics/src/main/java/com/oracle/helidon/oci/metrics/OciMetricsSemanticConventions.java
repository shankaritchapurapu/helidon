/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.Timer;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.helidon.webserver.observe.metrics.MetricsObserverConfig;
import io.helidon.webserver.observe.metrics.spi.AutoHttpMetricsProvider;

/**
 * HTTP semantic-convention metrics for the OCI metrics provider.
 */
@Service.Singleton
class OciMetricsSemanticConventions implements AutoHttpMetricsProvider {

    static final String RESPONSE_STATUS_CODE_PREFIX = "ResponseOut.StatusCode.";
    static final String RESPONSE_STATUS_FAMILY_PREFIX = "ResponseOut.StatusFamily.";
    static final String RESPONSE_TOTAL_COUNT = "ResponseOut.Count";
    static final String SUCCESS_RATE = "SuccessRate";
    static final String TIME = "Time";
    static final String RESOURCE_TIME = "ResourceTime";
    static final String WIRE_READ_TIME = "WireReadTime";
    static final String WIRE_WRITE_TIME = "WireWriteTime";
    static final String UNKNOWN_METHOD = "UnknownMethod";

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
        private final boolean detailedTimingEnabled;
        private final Optional<String> resourcePackagePrefix;
        private final ConcurrentMap<String, Counter> countersByName = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, DistributionSummary> summariesByName = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Timer> timersByName = new ConcurrentHashMap<>();

        private OciHttpMetricsFilter(OciMeterRegistry registry) {
            this.registry = registry;
            OciMetricsPublisherConfig publisherConfig = Optional.ofNullable(registry.publisher().prototype())
                    .orElseGet(() -> OciMetricsPublisherConfig.builder().buildPrototype());
            this.detailedTimingEnabled = publisherConfig.enableDetailedTimingAutoMetrics();
            this.resourcePackagePrefix = publisherConfig.resourcePackagePrefix();
        }

        @Override

        public void filter(FilterChain chain, RoutingRequest request, RoutingResponse response) {
            OciHttpEndpointMetricsContext context = context(request);
            if (context != null && !shouldTrack(context)) {
                chain.proceed();
                return;
            }
            WireWriteTiming wireWriteTiming = detailedTimingEnabled ? new WireWriteTiming(request) : null;
            if (detailedTimingEnabled) {
                request.streamFilter(stream -> new WireReadTimingInputStream(request, stream));
                response.streamFilter(stream -> new WireWriteTimingOutputStream(wireWriteTiming, stream));
            }
            long startNanos = System.nanoTime();
            response.whenSent(() -> {
                if (wireWriteTiming != null) {
                    wireWriteTiming.stop();
                }
                updateMetrics(request, response, System.nanoTime() - startNanos);
            });
            chain.proceed();
        }

        private void updateMetrics(RoutingRequest request, RoutingResponse response, long elapsedNanos) {
            OciHttpEndpointMetricsContext context = context(request);
            if (!shouldTrack(context)) {
                return;
            }
            int statusCode = response.status().code();
            String statusFamily = (statusCode / 100) + "XX";

            for (String scope : scopes(context)) {
                if (detailedTimingEnabled) {
                    timer(scope + "." + TIME).record(elapsedNanos, TimeUnit.NANOSECONDS);
                    if (context != null && context.hasResourceTiming()) {
                        timer(scope + "." + RESOURCE_TIME).record(context.resourceElapsedNanos(), TimeUnit.NANOSECONDS);
                    }
                }
                counter(scope + "." + RESPONSE_STATUS_CODE_PREFIX + statusCode + ".Count").increment();
                counter(scope + "." + RESPONSE_STATUS_FAMILY_PREFIX + statusFamily + ".Count").increment();
                counter(scope + "." + RESPONSE_TOTAL_COUNT).increment();
                successRate(scope).record(statusCode < 500 ? 1.0 : 0.0);
            }
        }

        private void recordWireTime(RoutingRequest request, String meterName, long elapsedNanos) {
            OciHttpEndpointMetricsContext context = context(request);
            if (!shouldTrack(context)) {
                return;
            }
            for (String scope : scopes(context)) {
                timer(scope + "." + meterName).record(elapsedNanos, TimeUnit.NANOSECONDS);
            }
        }

        private OciHttpEndpointMetricsContext context(RoutingRequest request) {
            return request.context()
                    .get(OciHttpEndpointMetricsContext.class)
                    .orElse(null);
        }

        private boolean shouldTrack(OciHttpEndpointMetricsContext context) {
            return resourcePackagePrefix
                    .map(prefix -> context != null
                            && context.fullyQualifiedResourceClassName().startsWith(prefix))
                    .orElse(true);
        }

        private Iterable<String> scopes(OciHttpEndpointMetricsContext context) {
            if (context == null) {
                return java.util.List.of(UNKNOWN_METHOD);
            }
            java.util.List<String> scopes = new java.util.ArrayList<>(1 + context.secondaryScopes().size());
            scopes.add(context.primaryScope());
            scopes.addAll(context.secondaryScopes());
            return scopes;
        }

        private Counter counter(String name) {
            return countersByName.computeIfAbsent(name, meterName -> registry.getOrCreate(OciCounter.builder(meterName)));
        }

        private Timer timer(String name) {
            return timersByName.computeIfAbsent(name, meterName -> registry.getOrCreate(OciTimer.builder(meterName)));
        }

        private DistributionSummary successRate(String scope) {
            return summariesByName.computeIfAbsent(scope + "." + SUCCESS_RATE,
                                                   meterName -> registry.getOrCreate(
                                                           OciDistributionSummary.builder(meterName)));
        }

        private final class WireReadTimingInputStream extends FilterInputStream {
            private final RoutingRequest request;
            private boolean started;
            private boolean stopped;
            private long startNanos;

            private WireReadTimingInputStream(RoutingRequest request, InputStream inputStream) {
                super(inputStream);
                this.request = request;
            }

            @Override
            public int read() throws IOException {
                start();
                int read = super.read();
                if (read == -1) {
                    stop();
                }
                return read;
            }

            @Override
            public int read(byte[] bytes) throws IOException {
                if (bytes.length == 0) {
                    return super.read(bytes);
                }
                start();
                int read = super.read(bytes);
                if (read == -1) {
                    stop();
                }
                return read;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                if (length == 0) {
                    return super.read(bytes, offset, length);
                }
                start();
                int read = super.read(bytes, offset, length);
                if (read == -1) {
                    stop();
                }
                return read;
            }

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    stop();
                }
            }

            private void start() {
                if (!started) {
                    started = true;
                    startNanos = System.nanoTime();
                }
            }

            private void stop() {
                if (started && !stopped) {
                    stopped = true;
                    recordWireTime(request, WIRE_READ_TIME, System.nanoTime() - startNanos);
                }
            }
        }

        /**
         * Allows either close on the stream or whenSent (if close was never explicitly invoked) to end the wire write timing.
         */
        private final class WireWriteTiming {
            private final RoutingRequest request;
            private boolean started;
            private boolean stopped;
            private long startNanos;

            private WireWriteTiming(RoutingRequest request) {
                this.request = request;
            }

            private void start() {
                if (!started) {
                    started = true;
                    startNanos = System.nanoTime();
                }
            }

            private void stop() {
                if (started && !stopped) {
                    stopped = true;
                    recordWireTime(request, WIRE_WRITE_TIME, System.nanoTime() - startNanos);
                }
            }
        }

        private final class WireWriteTimingOutputStream extends FilterOutputStream {
            private final WireWriteTiming timing;

            private WireWriteTimingOutputStream(WireWriteTiming timing, OutputStream outputStream) {
                super(outputStream);
                this.timing = timing;
            }

            @Override
            public void write(int value) throws IOException {
                timing.start();
                super.write(value);
            }

            @Override
            public void write(byte[] bytes) throws IOException {
                if (bytes.length > 0) {
                    timing.start();
                }
                out.write(bytes);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                if (length > 0) {
                    timing.start();
                }
                out.write(bytes, offset, length);
            }

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    timing.stop();
                }
            }
        }
    }
}
