/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsPublisher;

import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.model.MetricName;
import com.oracle.pic.telemetry.commons.metrics.model.Observation;

/**
 * Helidon metrics publisher that emits supported meter updates through OCI telemetry APIs.
 * <p>
 * The implementation intentionally relies on APIs available from {@code metrics-lib} rather than other older or
 * less-vigorously supported libraries (such as @{code service-core}) while maintaining semantic compatibility with the
 * {@code service-core} output.
 */
final class OciMetricsPublisher implements MetricsPublisher,
                                           RuntimeType.Api<OciMetricsPublisherConfig> {

    static final String TYPE = "oci";
    static final String VALUE_ATTRIBUTE = "value";
    static final String ACCUMULATOR_COMPACTIONS = "OciMetrics.Accumulator.Compactions.Count";
    static final String ACCUMULATOR_COMPACTED_SAMPLES = "OciMetrics.Accumulator.CompactedSamples.Count";
    static final String ACCUMULATOR_DROPPED_BUCKETS = "OciMetrics.Accumulator.DroppedBuckets.Count";
    static final String ACCUMULATOR_DROPPED_SAMPLES = "OciMetrics.Accumulator.DroppedSamples.Count";
    static final String ACCUMULATOR_PENDING_BUCKETS = "OciMetrics.Accumulator.PendingBuckets";
    static final String USER_AGENT_CARDINALITY_OVERFLOWS =
            "OciMetrics.AutoHttp.UserAgentCardinalityOverflows.Count";
    private static final System.Logger LOGGER = System.getLogger(OciMetricsPublisher.class.getName());

    private final OciMetricsPublisherConfig prototype;
    private final BiFunction<String, Meter, Boolean> metricFilter;
    private final Set<String> includedAttributes;
    private final Set<String> excludedAttributes;
    private final AtomicBoolean acceptingUpdates = new AtomicBoolean(true);
    private final OciAccumulatorStats accumulatorStats = new OciAccumulatorStats();
    private final LongAdder userAgentCardinalityOverflows = new LongAdder();

    OciMetricsPublisher() {
        this(null);
    }

    private OciMetricsPublisher(OciMetricsPublisherConfig prototype) {
        this.prototype = prototype;
        this.metricFilter = prototype == null ? ReporterMetricFilter.allowAll() : prototype.filter();
        this.includedAttributes = prototype == null ? Set.of() : prototype.includesAttributes();
        this.excludedAttributes = prototype == null ? Set.of() : prototype.excludesAttributes();
    }

    static OciMetricsPublisherConfig.Builder builder() {
        return OciMetricsPublisherConfig.builder();
    }

    static OciMetricsPublisher create(OciMetricsPublisherConfig prototype) {
        return new OciMetricsPublisher(prototype);
    }

    static OciMetricsPublisher create(Consumer<OciMetricsPublisherConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    @Override
    public OciMetricsPublisherConfig prototype() {
        return prototype;
    }

    @Override
    public String name() {
        return prototype == null ? TYPE : prototype.name().orElse(TYPE);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public boolean enabled() {
        return prototype == null || prototype.enabled();
    }

    boolean acceptingUpdates() {
        return acceptingUpdates.get();
    }

    void stop() {
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE, "Stopping OCI metrics publisher; name={0}", name());
        }
        acceptingUpdates.set(false);
    }

    void publishCounterDelta(OciCounter counter, long amount) {
        emitCount(counter, amount, System.currentTimeMillis(), "counter sample");
    }

    void publishFunctionalCounterDelta(OciFunctionalCounter<?> counter, long amount) {
        emitCount(counter, amount, System.currentTimeMillis(), "functional counter sample");
    }

    void publishObservations(Meter meter, List<Observation> observations, String updateDescription) {
        if (observations.isEmpty()) {
            return;
        }
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring {0} because publisher is stopped; meter={1}",
                           updateDescription,
                           meterDebug(meter));
            }
            return;
        }
        if (!shouldPublishValue(meter)) {
            logFiltered(meter, updateDescription);
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing {0}; meter={1}, observations={2}",
                       updateDescription,
                       meterDebug(meter),
                       observations.size());
        }
        publishObservations(metricName(meter), observations);
    }

    void publishGauge(OciGauge<?> gauge, OciGauge.Sample sample) {
        emitValue(gauge, sample.value(), 1, sample.timestampMillis(), "gauge sample");
    }

    void publishAccumulatorPressure(long pendingBuckets) {
        OciAccumulatorStats.Snapshot snapshot = accumulatorStats.drain();
        long cardinalityOverflows = userAgentCardinalityOverflows.sumThenReset();
        long timestampMillis = System.currentTimeMillis();
        publishInternalCount(ACCUMULATOR_COMPACTIONS, snapshot.compactions(), timestampMillis);
        publishInternalCount(ACCUMULATOR_COMPACTED_SAMPLES, snapshot.compactedSamples(), timestampMillis);
        publishInternalCount(ACCUMULATOR_DROPPED_BUCKETS, snapshot.droppedBuckets(), timestampMillis);
        publishInternalCount(ACCUMULATOR_DROPPED_SAMPLES, snapshot.droppedSamples(), timestampMillis);
        publishInternalCount(USER_AGENT_CARDINALITY_OVERFLOWS, cardinalityOverflows, timestampMillis);
        if (pendingBuckets > 0L || !snapshot.isEmpty()) {
            publishInternalValue(ACCUMULATOR_PENDING_BUCKETS, pendingBuckets, 1, timestampMillis);
        }
    }

    void recordUserAgentCardinalityOverflow() {
        userAgentCardinalityOverflows.increment();
    }

    OciAccumulatorStats accumulatorStats() {
        return accumulatorStats;
    }

    boolean shouldPublish(Meter meter) {
        return Boolean.TRUE.equals(metricFilter.apply(meter.id().name(), meter));
    }

    boolean shouldPublish(Meter meter, String attribute) {
        return shouldPublish(meter) && shouldPublishAttribute(attribute);
    }

    boolean shouldPublishValue(Meter meter) {
        return shouldPublish(meter, VALUE_ATTRIBUTE);
    }

    boolean shouldCreateValueMeter(String name, Meter.Type type) {
        if (!shouldPublishAttribute(VALUE_ATTRIBUTE)) {
            return false;
        }
        return !(metricFilter instanceof ReporterMetricFilter)
                || Boolean.TRUE.equals(metricFilter.apply(name, new InternalMeter(name, type)));
    }

    boolean shouldPublishAttribute(String attribute) {
        return !excludedAttributes.contains(attribute)
                && (includedAttributes.isEmpty() || includedAttributes.contains(attribute));
    }

    private void emitCount(Meter meter, long amount, long timestampMillis, String updateDescription) {
        if (amount <= 0L) {
            return;
        }
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring {0} because publisher is stopped; meter={1}",
                           updateDescription,
                           meterDebug(meter));
            }
            return;
        }
        if (!shouldPublishValue(meter)) {
            logFiltered(meter, updateDescription);
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing {0}; meter={1}, count={2}",
                       updateDescription,
                       meterDebug(meter),
                       amount);
        }
        emitCount(metricName(meter), amount, timestampMillis);
    }

    private void emitValue(Meter meter, double value, int count, long timestampMillis, String updateDescription) {
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring {0} because publisher is stopped; meter={1}",
                           updateDescription,
                           meterDebug(meter));
            }
            return;
        }
        if (!shouldPublishValue(meter)) {
            logFiltered(meter, updateDescription);
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing {0}; meter={1}, value={2}",
                       updateDescription,
                       meterDebug(meter),
                       value);
        }
        Metrics.emit(metricName(meter), value, count, timestampMillis);
    }

    private void publishInternalCount(String name, long amount, long timestampMillis) {
        if (amount <= 0L || !acceptingUpdates.get() || !shouldPublishInternalValue(name, Meter.Type.COUNTER)) {
            return;
        }
        emitCount(MetricName.of(name), amount, timestampMillis);
    }

    private void publishInternalValue(String name, double value, int count, long timestampMillis) {
        if (!acceptingUpdates.get() || !shouldPublishInternalValue(name, Meter.Type.GAUGE)) {
            return;
        }
        Metrics.emit(MetricName.of(name), value, count, timestampMillis);
    }

    private boolean shouldPublishInternalValue(String name, Meter.Type type) {
        return shouldPublishAttribute(VALUE_ATTRIBUTE)
                && Boolean.TRUE.equals(metricFilter.apply(name, new InternalMeter(name, type)));
    }

    private static void emitCount(MetricName metricName, long amount, long timestampMillis) {
        long remaining = amount;
        while (remaining > 0L) {
            int count = (int) Math.min(Integer.MAX_VALUE, remaining);
            Metrics.emit(metricName, 1D, count, timestampMillis);
            remaining -= count;
        }
    }

    private static void publishObservations(MetricName metricName, List<Observation> observations) {
        observations.forEach(observation -> Metrics.emit(metricName,
                                                         observation.getValue(),
                                                         observation.getCount(),
                                                         observation.getTimestamp()));
    }

    private MetricName metricName(Meter meter) {
        return OciMetricNames.create(meter);
    }

    private void logFiltered(Meter meter, String updateDescription) {
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Ignoring {0} because metric is excluded by reporter config; meter={1}",
                       updateDescription,
                       meterDebug(meter));
        }
    }

    private static String meterDebug(Meter meter) {
        return meter.id().name() + meter.id().tagsMap() + meter.scope().map(scope -> "/" + scope).orElse("");
    }

    /**
     * Minimal meter view used only when applying configured publisher filters to internally emitted metrics.
     * <p>
     * Accumulator-pressure metrics are not registered Helidon meters, but user-provided custom programmatic filters receive a
     * {@link Meter} argument. This stand-in avoids passing {@code null} while still exposing the name and type a filter
     * might reasonably inspect.
     */
    private record InternalMeter(String name, Meter.Type type) implements Meter {
        @Override
        public Id id() {
            return new InternalId(name);
        }

        @Override
        public Optional<String> baseUnit() {
            return Optional.empty();
        }

        @Override
        public Optional<String> description() {
            return Optional.empty();
        }

        @Override
        public Optional<String> scope() {
            return Optional.empty();
        }

        @Override
        public <R> R unwrap(Class<? extends R> type) {
            if (type.isInstance(this)) {
                return type.cast(this);
            }
            throw new IllegalArgumentException("Unsupported unwrap type: " + type.getName());
        }
    }

    private record InternalId(String name) implements Meter.Id {
        @Override
        public Iterable<io.helidon.metrics.api.Tag> tags() {
            return List.of();
        }
    }

}
