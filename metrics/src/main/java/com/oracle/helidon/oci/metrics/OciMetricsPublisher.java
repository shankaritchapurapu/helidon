/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.metrics.api.FunctionalCounter;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsPublisher;

import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.Sensor;
import com.oracle.pic.telemetry.commons.metrics.model.MetricName;

/**
 * Helidon metrics publisher that emits supported meter updates through OCI telemetry APIs.
 */
final class OciMetricsPublisher implements MetricsPublisher,
                                           RuntimeType.Api<OciMetricsPublisherConfig> {

    static final String TYPE = "oci";
    static final String VALUE_ATTRIBUTE = "value";
    static final String MEAN_ATTRIBUTE = "mean";
    static final String ONE_MINUTE_RATE_ATTRIBUTE = "m1_rate";
    private static final System.Logger LOGGER = System.getLogger(OciMetricsPublisher.class.getName());

    private final OciMetricsPublisherConfig prototype;
    private final BiFunction<String, Meter, Boolean> metricFilter;
    private final Set<String> includedAttributes;
    private final Set<String> excludedAttributes;
    private final AtomicBoolean acceptingUpdates = new AtomicBoolean(true);

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

    void stop() {
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE, "Stopping OCI metrics publisher; name={0}", name());
        }
        acceptingUpdates.set(false);
    }

    void publishCounterDelta(OciCounter counter, long amount) {
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring counter sample because publisher is stopped; meter={0}",
                           meterDebug(counter));
            }
            return;
        }
        if (!shouldPublishValue(counter)) {
            logFiltered(counter, "counter sample");
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing counter sample; meter={0}, amount={1}",
                       meterDebug(counter),
                       amount);
        }
        Metrics.sensor(metricName(counter)).record(amount);
    }

    void publishTimerRate(OciTimer timer, double value) {
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring timer rate sample because publisher is stopped; meter={0}",
                           meterDebug(timer));
            }
            return;
        }
        if (!shouldPublishTimerRate(timer)) {
            logFiltered(timer, "timer rate sample");
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing timer rate sample; meter={0}, value={1}",
                       meterDebug(timer),
                       value);
        }
        Metrics.sensor(metricName(timer)).singleValue(Sensor.AggregationType.Last).record(value);
    }

    void publishDistributionSummaryMean(OciDistributionSummary summary, double value) {
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring distribution summary mean sample because publisher is stopped; meter={0}",
                           meterDebug(summary));
            }
            return;
        }
        if (!shouldPublishDistributionSummaryMean(summary)) {
            logFiltered(summary, "distribution summary mean sample");
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing distribution summary mean sample; meter={0}, value={1}",
                       meterDebug(summary),
                       value);
        }
        Metrics.sensor(metricName(summary)).singleValue(Sensor.AggregationType.Last).record(value);
    }

    void publishFunctionalCounter(FunctionalCounter counter, long value) {
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring functional counter sample because publisher is stopped; meter={0}",
                           meterDebug(counter));
            }
            return;
        }
        if (!shouldPublishValue(counter)) {
            logFiltered(counter, "functional counter sample");
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing functional counter sample; meter={0}, value={1}",
                       meterDebug(counter),
                       value);
        }
        Metrics.sensor(metricName(counter)).singleValue(Sensor.AggregationType.Last).record(value);
    }

    void publishGauge(OciGauge<?> gauge, double value) {
        if (!acceptingUpdates.get()) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Ignoring gauge sample because publisher is stopped; meter={0}",
                           meterDebug(gauge));
            }
            return;
        }
        if (!shouldPublishValue(gauge)) {
            logFiltered(gauge, "gauge sample");
            return;
        }
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Publishing gauge sample; meter={0}, value={1}",
                       meterDebug(gauge),
                       value);
        }
        Metrics.sensor(metricName(gauge)).singleValue(Sensor.AggregationType.Last).record(value);
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

    boolean shouldPublishTimerRate(Meter meter) {
        return shouldPublish(meter, ONE_MINUTE_RATE_ATTRIBUTE);
    }

    boolean shouldPublishDistributionSummaryMean(Meter meter) {
        return shouldPublish(meter, MEAN_ATTRIBUTE);
    }

    boolean shouldPublishAttribute(String attribute) {
        return !excludedAttributes.contains(attribute)
                && (includedAttributes.isEmpty() || includedAttributes.contains(attribute));
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

}
