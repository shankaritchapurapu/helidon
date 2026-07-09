/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import io.helidon.http.HeaderNames;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.Tag;
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

    private static final System.Logger LOGGER = System.getLogger(OciMetricsSemanticConventions.class.getName());
    private static final AtomicInteger ASYNC_UPDATES_IN_FLIGHT = new AtomicInteger();

    static final String RESPONSE_STATUS_CODE_PREFIX = "ResponseOut.StatusCode.";
    static final String RESPONSE_STATUS_FAMILY_PREFIX = "ResponseOut.StatusFamily.";
    static final String RESPONSE_TOTAL_COUNT = "ResponseOut.Count";
    static final String REQUEST_CLIENT_PREFIX = "Request.Client.";
    static final String OTHER_CLIENT = "OTHER";
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

    static boolean asyncUpdatesInFlight() {
        return ASYNC_UPDATES_IN_FLIGHT.get() > 0;
    }

    static boolean awaitAsyncUpdates(Duration timeout) {
        if (!asyncUpdatesInFlight()) {
            return true;
        }
        if (timeout.isZero() || timeout.isNegative()) {
            return false;
        }

        long deadline = System.nanoTime() + timeout.toNanos();
        while (asyncUpdatesInFlight()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) {
                return false;
            }
            LockSupport.parkNanos(Math.min(TimeUnit.MILLISECONDS.toNanos(10), remaining));
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
        }
        return true;
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
                || !ociAutoHttpMetricsEnabled(ociMeterRegistry)
                ? Optional.empty()
                : Optional.of(new OciHttpMetricsFilter(ociMeterRegistry));
    }

    private static boolean ociAutoHttpMetricsEnabled(OciMeterRegistry registry) {
        OciMetricsPublisherConfig publisherConfig = Optional.ofNullable(registry.publisher().prototype())
                .orElseGet(() -> OciMetricsPublisherConfig.builder().buildPrototype());
        return publisherConfig.enabled() && publisherConfig.autoHttp().enabled();
    }

    private static final class OciHttpMetricsFilter implements Filter {

        private final OciMeterRegistry registry;
        private final boolean detailedTimingEnabled;
        private final boolean userAgentMetricsEnabled;
        private final Optional<String> resourcePackagePrefix;
        private final Optional<OciAutoHttpRuntimeDimensionConfig> runtimeDimensionConfig;
        private final UserAgentParserCollator userAgentParserCollator;
        private final BoundedCardinalityLimiter<MeterCacheKey> userAgentSeriesLimiter;
        private final ConcurrentMap<MeterCacheKey, OciCounter> counters = new ConcurrentHashMap<>();
        private final ConcurrentMap<MeterCacheKey, OciDistributionSummary> distributionSummaries = new ConcurrentHashMap<>();
        private final ConcurrentMap<MeterCacheKey, OciTimer> timers = new ConcurrentHashMap<>();

        private OciHttpMetricsFilter(OciMeterRegistry registry) {
            this.registry = registry;
            OciMetricsPublisherConfig publisherConfig = Optional.ofNullable(registry.publisher().prototype())
                    .orElseGet(() -> OciMetricsPublisherConfig.builder().buildPrototype());
            this.detailedTimingEnabled = publisherConfig.enableDetailedTimingAutoMetrics();
            this.userAgentMetricsEnabled = publisherConfig.autoHttp().userAgentMetricsEnabled();
            this.resourcePackagePrefix = publisherConfig.resourcePackagePrefix();
            this.runtimeDimensionConfig = publisherConfig.autoHttp().runtimeDimension();
            this.userAgentParserCollator = userAgentMetricsEnabled ? UserAgentParserCollator.createDefault() : null;
            this.userAgentSeriesLimiter = new BoundedCardinalityLimiter<>(
                    publisherConfig.autoHttp().maxUserAgentSeries());
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
                long sentNanos = System.nanoTime();
                if (wireWriteTiming != null) {
                    wireWriteTiming.stop(sentNanos);
                }
                /*
                Primarily lets tests know when asynchronous metric updates have completed.
                 */
                ASYNC_UPDATES_IN_FLIGHT.incrementAndGet();
                try {
                    /*
                    Arrange to do the rest of the work asynchronously to avoid delaying the transmission of the now-completed
                    response output.
                     */
                    Thread.ofVirtual()
                            .name("oci-http-metrics-update-")
                            .start(() -> {
                                try {
                                    updateMetrics(request, response, sentNanos - startNanos);
                                } catch (RuntimeException e) {
                                    LOGGER.log(System.Logger.Level.WARNING, "Error updating automatic HTTP metrics", e);
                                } finally {
                                    ASYNC_UPDATES_IN_FLIGHT.decrementAndGet();
                                }
                            });
                } catch (RuntimeException e) {
                    ASYNC_UPDATES_IN_FLIGHT.decrementAndGet();
                    throw e;
                }
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
            boolean skipResponseStatusMetrics = skipResponseStatusMetrics(request);
            RuntimeDimension runtimeDimension = runtimeDimension(request);

            for (String scope : scopes(context)) {
                if (detailedTimingEnabled) {
                    timer(scope + "." + TIME, runtimeDimension.tags()).record(elapsedNanos, TimeUnit.NANOSECONDS);
                    if (context != null && context.hasResourceTiming()) {
                        timer(scope + "." + RESOURCE_TIME, runtimeDimension.tags())
                                .record(context.resourceElapsedNanos(), TimeUnit.NANOSECONDS);
                    }
                }
                if (!skipResponseStatusMetrics) {
                    counter(countMetricName(scope, RESPONSE_STATUS_CODE_PREFIX + statusCode + ".Count", runtimeDimension),
                            runtimeDimension.tags()).increment();
                    counter(countMetricName(scope,
                                            RESPONSE_STATUS_FAMILY_PREFIX + statusFamily + ".Count",
                                            runtimeDimension),
                            runtimeDimension.tags()).increment();
                }
                counter(countMetricName(scope, RESPONSE_TOTAL_COUNT, runtimeDimension), runtimeDimension.tags()).increment();
                distributionSummary(scope + "." + SUCCESS_RATE, runtimeDimension.tags())
                        .record(statusCode < 500 ? 1.0 : 0.0);
                recordUserAgentMetrics(request, scope, statusFamily, skipResponseStatusMetrics, runtimeDimension);
            }
        }

        private void recordUserAgentMetrics(RoutingRequest request,
                                            String scope,
                                            String statusFamily,
                                            boolean skipResponseStatusMetrics,
                                            RuntimeDimension runtimeDimension) {
            if (!userAgentMetricsEnabled) {
                return;
            }

            List<String> clientAggregations = userAgentAggregations(request);
            if (clientAggregations.isEmpty()) {
                return;
            }

            incrementUserAgentCounters(scope,
                                       clientAggregations.getFirst(),
                                       statusFamily,
                                       skipResponseStatusMetrics,
                                       runtimeDimension);

            boolean overflow = false;
            for (String clientAggregation : clientAggregations.subList(1, clientAggregations.size())) {
                List<MeterCacheKey> keys = userAgentCounterKeys(scope,
                                                               clientAggregation,
                                                               statusFamily,
                                                               skipResponseStatusMetrics,
                                                               runtimeDimension).stream()
                        .filter(this::shouldCreateCounter)
                        .toList();
                if (keys.isEmpty()) {
                    continue;
                }
                if (userAgentSeriesLimiter.tryAdmit(keys)) {
                    keys.forEach(this::incrementCounter);
                } else {
                    overflow = true;
                }
            }
            if (overflow) {
                incrementUserAgentCounters(scope,
                                           OTHER_CLIENT,
                                           statusFamily,
                                           skipResponseStatusMetrics,
                                           runtimeDimension);
                registry.publisher().recordUserAgentCardinalityOverflow();
            }
        }

        private List<String> userAgentAggregations(RoutingRequest request) {
            String userAgent = request.headers().first(HeaderNames.USER_AGENT).orElse(null);
            UserAgentInfo userAgentInfo = userAgent == null
                    ? UserAgentInfo.UNDEFINED
                    : userAgentParserCollator.parse(userAgent);
            return userAgentInfo.toAggregatedMetricStrings();
        }

        private void incrementUserAgentCounters(String scope,
                                                String clientAggregation,
                                                String statusFamily,
                                                boolean skipResponseStatusMetrics,
                                                RuntimeDimension runtimeDimension) {
            userAgentCounterKeys(scope,
                                 clientAggregation,
                                 statusFamily,
                                 skipResponseStatusMetrics,
                                 runtimeDimension).stream()
                    .filter(this::shouldCreateCounter)
                    .forEach(this::incrementCounter);
        }

        private List<MeterCacheKey> userAgentCounterKeys(String scope,
                                                         String clientAggregation,
                                                         String statusFamily,
                                                         boolean skipResponseStatusMetrics,
                                                         RuntimeDimension runtimeDimension) {
            List<MeterCacheKey> result = new ArrayList<>(2);
            if (!skipResponseStatusMetrics) {
                result.add(MeterCacheKey.create(
                        countMetricName(scope,
                                        REQUEST_CLIENT_PREFIX + clientAggregation + "." + statusFamily + ".Count",
                                        runtimeDimension),
                        runtimeDimension.tags()));
            }
            result.add(MeterCacheKey.create(
                    countMetricName(scope, REQUEST_CLIENT_PREFIX + clientAggregation + ".Count", runtimeDimension),
                    runtimeDimension.tags()));
            return List.copyOf(result);
        }

        private void incrementCounter(MeterCacheKey key) {
            counter(key).increment();
        }

        private boolean shouldCreateCounter(MeterCacheKey key) {
            return registry.publisher().shouldCreateValueMeter(key.name(), Meter.Type.COUNTER);
        }

        private boolean skipResponseStatusMetrics(RoutingRequest request) {
            return request.context()
                    .get(OciHttpEndpointMetricsContext.SKIP_RESPONSE_STATUS_METRICS, Boolean.class)
                    .orElse(false);
        }

        private void recordWireTime(RoutingRequest request, String meterName, long elapsedNanos) {
            OciHttpEndpointMetricsContext context = context(request);
            if (!shouldTrack(context)) {
                return;
            }
            RuntimeDimension runtimeDimension = runtimeDimension(request);
            for (String scope : scopes(context)) {
                timer(scope + "." + meterName, runtimeDimension.tags()).record(elapsedNanos, TimeUnit.NANOSECONDS);
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

        private RuntimeDimension runtimeDimension(RoutingRequest request) {
            if (runtimeDimensionConfig.isEmpty()) {
                return RuntimeDimension.NONE;
            }
            OciAutoHttpRuntimeDimensionConfig config = runtimeDimensionConfig.get();
            Optional<String> requestValue = request.context().get(config.propertyName(), String.class);
            Optional<String> dimensionValue = requestValue.isPresent()
                    ? requestValue
                    : config.defaultDimension().filter(value -> !value.isEmpty());
            return dimensionValue
                    .map(value -> new RuntimeDimension(Map.of(config.dimensionName(), value),
                                                       value.isEmpty() ? "" : value + "."))
                    .orElse(RuntimeDimension.NONE);
        }

        private String countMetricName(String scope, String suffix, RuntimeDimension runtimeDimension) {
            // Raw runtime-dimension values are intentionally inserted into count metric names for service-core parity.
            return scope + "." + runtimeDimension.countNamePrefix() + suffix;
        }

        private OciCounter counter(String name, Map<String, String> tags) {
            return counter(MeterCacheKey.create(name, tags));
        }

        private OciCounter counter(MeterCacheKey key) {
            return counters.computeIfAbsent(key,
                                            ignored -> {
                                                OciCounter.Builder builder = OciCounter.builder(key.name());
                                                addTags(builder, key.tags());
                                                return (OciCounter) registry.getOrCreate(builder);
                                            });
        }

        private OciDistributionSummary distributionSummary(String name, Map<String, String> tags) {
            return distributionSummaries.computeIfAbsent(MeterCacheKey.create(name, tags),
                                                         ignored -> {
                                                             OciDistributionSummary.Builder builder =
                                                                     OciDistributionSummary.builder(name);
                                                             addTags(builder, tags);
                                                             return (OciDistributionSummary) registry.getOrCreate(builder);
                                                         });
        }

        private OciTimer timer(String name, Map<String, String> tags) {
            return timers.computeIfAbsent(MeterCacheKey.create(name, tags),
                                          ignored -> {
                                              OciTimer.Builder builder = OciTimer.builder(name);
                                              addTags(builder, tags);
                                              return (OciTimer) registry.getOrCreate(builder);
                                          });
        }

        private void addTags(AbstractOciMeterBuilder<?, ?> builder, Map<String, String> tags) {
            tags.forEach((key, value) -> builder.addTag(Tag.create(key, value)));
        }

        private record RuntimeDimension(Map<String, String> tags, String countNamePrefix) {
            private static final RuntimeDimension NONE = new RuntimeDimension(Map.of(), "");
        }

        private record MeterCacheKey(String name, Map<String, String> tags) {
            private static MeterCacheKey create(String name, Map<String, String> tags) {
                return new MeterCacheKey(name, Map.copyOf(tags));
            }
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
                stop(System.nanoTime());
            }

            private void stop(long stopNanos) {
                if (started && !stopped) {
                    stopped = true;
                    recordWireTime(request, WIRE_WRITE_TIME, stopNanos - startNanos);
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
