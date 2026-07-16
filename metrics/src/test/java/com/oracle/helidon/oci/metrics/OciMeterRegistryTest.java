/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.helidon.config.Config;
import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionStatisticsConfig;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.FunctionalCounter;
import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Timer;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.model.Observation;
import com.oracle.pic.telemetry.commons.metrics.model.TimeSeries;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

class OciMeterRegistryTest {

    @Test
    void counterMaintainsCountAndReusesRegistration() {
        OciMeterRegistry registry = createRegistry();

        OciCounter first = (OciCounter) registry.getOrCreate(OciCounter.builder("requests")
                                                            .addTag(io.helidon.metrics.api.Tag.create("k", "v")));
        Counter second = registry.getOrCreate(OciCounter.builder("requests")
                                                      .addTag(io.helidon.metrics.api.Tag.create("k", "v")));

        first.increment(3);

        assertThat(second, sameInstance(first));
        assertThat(first.drainDelta(), is(3L));
    }

    @Test
    void counterDrainsOnlyPositiveDeltas() {
        OciMeterRegistry registry = createRegistry();
        OciCounter counter = (OciCounter) registry.getOrCreate(OciCounter.builder("sampled.requests"));

        counter.increment(3);

        assertThat(counter.drainDelta(), is(3L));
        assertThat(counter.drainDelta(), is(0L));

        counter.increment(2);

        assertThat(counter.drainDelta(), is(2L));
    }

    @Test
    void excludedCounterDoesNotRetainPendingDelta() {
        OciMeterRegistry registry = createRegistry(Clock.system(),
                                                   OciMetricsPublisherConfig.builder()
                                                           .enabled(true)
                                                           .reporter(new TestMetricReporter())
                                                           .reporterConfig(overlayReporterConfig())
                                                           .defaultDimensions(Map.of())
                                                           .excludes(Set.of("requests.excluded"))
                                                           .buildPrototype());
        OciCounter counter = (OciCounter) registry.getOrCreate(OciCounter.builder("requests.excluded"));

        counter.increment(3);

        assertThat(counter.drainDelta(), is(0L));
    }

    @Test
    void gaugeSamplesOnAccessWithoutCaching() {
        OciMeterRegistry registry = createRegistry();
        AtomicLong value = new AtomicLong(5);
        AtomicLong calls = new AtomicLong();
        Gauge<Long> gauge = registry.getOrCreate(OciGauge.builder("queue.size", () -> {
            calls.incrementAndGet();
            return value.get();
        }));

        assertThat(gauge.value(), is(5L));
        assertThat(calls.get(), is(1L));
        value.set(7);
        assertThat(gauge.value(), is(7L));
        assertThat(calls.get(), is(2L));
    }

    @Test
    void timerTracksCountTotalAndMax() {
        OciMeterRegistry registry = createRegistry();
        Timer timer = registry.getOrCreate(OciTimer.builder("latency"));

        timer.record(Duration.ofMillis(10));
        timer.record(20, TimeUnit.MILLISECONDS);

        assertThat(timer.count(), is(2L));
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS), is(30D));
        assertThat(timer.mean(TimeUnit.MILLISECONDS), is(15D));
        assertThat(timer.max(TimeUnit.MILLISECONDS), is(20D));
        assertThat(timer.snapshot(), notNullValue());
    }

    @Test
    void timerDrainsSamplesOnceBySecond() {
        TestClock clock = new TestClock();
        OciMeterRegistry registry = createRegistry(clock);
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("latency.samples"));

        timer.record(Duration.ofMillis(10));
        timer.record(Duration.ofMillis(20));
        clock.advance(Duration.ofSeconds(1));
        timer.record(Duration.ofMillis(30));

        assertThat(timer.drainClosedIntervalSamples(clock.wallTime()),
                   contains(new Observation(0L, 10D, 1),
                            new Observation(0L, 20D, 1)));
        assertThat(timer.drainClosedIntervalSamples(clock.wallTime()), empty());
        assertThat(timer.drainAllIntervalSamples(), contains(new Observation(1_000L, 30D, 1)));
    }

    @Test
    void disabledTimerDoesNotRetainIntervalSamples() {
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .scoping(scoping -> scoping.putScope(Meter.Scope.DEFAULT,
                                                     scope -> scope.name(Meter.Scope.DEFAULT)
                                                             .exclude("latency\\.disabled")))
                .build();
        OciMeterRegistry registry = (OciMeterRegistry) createFactory(metricsConfig).createMeterRegistry(Clock.system(),
                                                                                                       metricsConfig);
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("latency.disabled"));

        timer.record(Duration.ofMillis(10));
        timer.record(Duration.ofMillis(20));

        assertThat(timer.enabled(), is(false));
        assertThat(timer.drainAllIntervalSamples(), empty());
    }

    @Test
    void publisherDisabledTimerDoesNotRetainIntervalSamples() {
        OciMeterRegistry registry = createRegistry(Clock.system(),
                                                   OciMetricsPublisherConfig.builder()
                                                           .enabled(false)
                                                           .reporterConfig(overlayReporterConfig())
                                                           .defaultDimensions(Map.of())
                                                           .buildPrototype());
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("latency.publisher.disabled"));

        timer.record(Duration.ofMillis(10));

        assertThat(timer.enabled(), is(true));
        assertThat(timer.drainAllIntervalSamples(), empty());
    }

    @Test
    void excludedTimerDoesNotRetainIntervalSamplesOrPressureStats() {
        OciMeterRegistry registry = createRegistry(Clock.system(),
                                                   OciMetricsPublisherConfig.builder()
                                                           .enabled(true)
                                                           .reporter(new TestMetricReporter())
                                                           .reporterConfig(overlayReporterConfig())
                                                           .defaultDimensions(Map.of())
                                                           .excludes(Set.of("latency.excluded"))
                                                           .accumulators(accumulators -> accumulators
                                                                   .maxRawTimerSamplesPerSecond(2))
                                                           .buildPrototype());
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("latency.excluded"));

        timer.record(Duration.ofMillis(10));
        timer.record(Duration.ofMillis(20));
        timer.record(Duration.ofMillis(30));

        assertThat(timer.drainAllIntervalSamples(), empty());
        assertThat(timer.pendingBucketCount(), is(0));
        assertThat(registry.publisher().accumulatorStats().drain().isEmpty(), is(true));
    }

    @Test
    void timerDrainsExactServiceCoreStyleObservations() {
        TestClock clock = new TestClock();
        OciMeterRegistry registry = createRegistry(clock);
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("latency.exact"));

        timer.record(Duration.ofMillis(10));
        timer.record(Duration.ofMillis(20));
        timer.record(Duration.ofMillis(40));
        timer.record(Duration.ofMillis(40));

        assertThat(timer.drainAllIntervalSamples(),
                   contains(new Observation(0L, 10D, 1),
                            new Observation(0L, 20D, 1),
                            new Observation(0L, 40D, 1),
                            new Observation(0L, 40D, 1)));
    }

    @Test
    void timerCompactsAboveRawCapPreservingCountSumMinAndMax() {
        OciMeterRegistry registry = createRegistry(Clock.system(),
                                                   OciMetricsPublisherConfig.builder()
                                                           .enabled(true)
                                                           .reporter(new TestMetricReporter())
                                                           .reporterConfig(overlayReporterConfig())
                                                           .defaultDimensions(Map.of())
                                                           .accumulators(accumulators -> accumulators
                                                                   .maxRawTimerSamplesPerSecond(2))
                                                           .buildPrototype());
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("latency.compact"));

        timer.record(Duration.ofMillis(10));
        timer.record(Duration.ofMillis(20));
        timer.record(Duration.ofMillis(40));
        timer.record(Duration.ofMillis(50));

        List<Observation> observations = timer.drainAllIntervalSamples();

        assertThat(observations,
                   contains(new Observation(observations.getFirst().getTimestamp(), 10D, 1),
                            new Observation(observations.getFirst().getTimestamp(), 50D, 1),
                            new Observation(observations.getFirst().getTimestamp(), 30D, 2)));
    }

    @Test
    void distributionSummaryTracksIntervalSamples() {
        OciMeterRegistry registry = createRegistry();
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("payload", DistributionStatisticsConfig.builder()));

        summary.record(10D);
        summary.record(20D);

        List<Observation> observations = summary.drainAllIntervalSamples();

        assertThat(observations,
                   contains(new Observation(observations.getFirst().getTimestamp(), 10D, 1),
                            new Observation(observations.getFirst().getTimestamp(), 20D, 1)));
    }

    @Test
    void distributionSummaryDrainsExactIntervalSamples() {
        TestClock clock = new TestClock();
        OciMeterRegistry registry = createRegistry(clock);
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("sampled.payload", DistributionStatisticsConfig.builder()));

        summary.record(10D);
        summary.record(20D);

        List<Observation> observations = summary.drainAllIntervalSamples();

        assertThat(observations,
                   contains(new Observation(0L, 10D, 1),
                            new Observation(0L, 20D, 1)));
        assertThat(summary.drainAllIntervalSamples(), empty());
    }

    @Test
    void distributionSummaryCompactsAboveRawCapPreservingCountSumMinAndMax() {
        OciMeterRegistry registry = createRegistry(Clock.system(),
                                                   OciMetricsPublisherConfig.builder()
                                                           .enabled(true)
                                                           .reporter(new TestMetricReporter())
                                                           .reporterConfig(overlayReporterConfig())
                                                           .defaultDimensions(Map.of())
                                                           .accumulators(accumulators -> accumulators
                                                                   .maxRawSummarySamplesPerSecond(2))
                                                           .buildPrototype());
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("sampled.payload", DistributionStatisticsConfig.builder()));

        summary.record(10D);
        summary.record(20D);
        summary.record(40D);
        summary.record(50D);

        List<Observation> observations = summary.drainAllIntervalSamples();

        assertThat(observations,
                   contains(new Observation(observations.getFirst().getTimestamp(), 10D, 1),
                            new Observation(observations.getFirst().getTimestamp(), 50D, 1),
                            new Observation(observations.getFirst().getTimestamp(), 30D, 2)));
    }

    @Test
    void excludedDistributionSummaryDoesNotRetainIntervalSamplesOrPressureStats() {
        OciMeterRegistry registry = createRegistry(Clock.system(),
                                                   OciMetricsPublisherConfig.builder()
                                                           .enabled(true)
                                                           .reporter(new TestMetricReporter())
                                                           .reporterConfig(overlayReporterConfig())
                                                           .defaultDimensions(Map.of())
                                                           .excludes(Set.of("payload.excluded"))
                                                           .accumulators(accumulators -> accumulators
                                                                   .maxRawSummarySamplesPerSecond(2))
                                                           .buildPrototype());
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("payload.excluded", DistributionStatisticsConfig.builder()));

        summary.record(10D);
        summary.record(20D);
        summary.record(30D);

        assertThat(summary.drainAllIntervalSamples(), empty());
        assertThat(summary.pendingBucketCount(), is(0));
        assertThat(registry.publisher().accumulatorStats().drain().isEmpty(), is(true));
    }

    @Test
    void customFilterDoesNotRunDuringEventDrivenMeterMutation() {
        AtomicLong filterCalls = new AtomicLong();
        String metricName = "custom.filter.counter";
        TestClock clock = new TestClock();
        OciMeterRegistry registry = createRegistry(clock,
                                                   OciMetricsPublisherConfig.builder()
                                                           .enabled(true)
                                                           .reporter(new TestMetricReporter())
                                                           .reporterConfig(overlayReporterConfig())
                                                           .defaultDimensions(Map.of())
                                                           .filter((name, meter) -> {
                                                               filterCalls.incrementAndGet();
                                                               return !metricName.equals(name);
                                                           })
                                                           .buildPrototype());
        OciCounter counter = (OciCounter) registry.getOrCreate(OciCounter.builder(metricName));
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("custom.filter.timer"));
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("custom.filter.summary", DistributionStatisticsConfig.builder()));

        counter.increment(3);
        timer.record(Duration.ofMillis(10));
        summary.record(20D);

        assertThat(filterCalls.get(), is(0L));
        registry.publisher().publishCounterDelta(counter, counter.drainDelta());
        assertThat(filterCalls.get(), is(1L));
        assertThat(timer.drainAllIntervalSamples(), contains(new Observation(0L, 10D, 1)));
        assertThat(summary.drainAllIntervalSamples(), contains(new Observation(0L, 20D, 1)));
    }

    @Test
    void stoppedPublisherPreventsEventDrivenAccumulation() {
        TestClock clock = new TestClock();
        OciMeterRegistry registry = createRegistry(clock);
        OciCounter counter = (OciCounter) registry.getOrCreate(OciCounter.builder("stopped.counter"));
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("stopped.timer"));
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("stopped.summary", DistributionStatisticsConfig.builder()));

        registry.publisher().stop();

        counter.increment(3);
        timer.record(Duration.ofMillis(10));
        summary.record(20D);

        assertThat(counter.drainDelta(), is(0L));
        assertThat(timer.drainAllIntervalSamples(), empty());
        assertThat(summary.drainAllIntervalSamples(), empty());
    }

    @Test
    void distributionSummarySamplesNormalizedIntervalMean() {
        TestClock clock = new TestClock();
        OciMeterRegistry registry = createRegistry(clock);
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("sampled.kilobytes")
                        .baseUnit("kilobytes"));

        summary.record(1D);
        summary.record(2D);

        assertThat(summary.drainAllIntervalSamples(),
                   contains(new Observation(0L, 1024D, 1),
                            new Observation(0L, 2048D, 1)));
    }

    @Test
    void functionalCounterSamplesCurrentValueOnAccess() {
        OciMeterRegistry registry = createRegistry();
        AtomicLong state = new AtomicLong(11);
        FunctionalCounter counter = registry.getOrCreate(OciFunctionalCounter.builder("in.flight",
                                                                                      state,
                                                                                      AtomicLong::get));

        assertThat(counter.count(), is(11L));
        state.set(15);
        assertThat(counter.count(), is(15L));
    }

    @Test
    void functionalCounterReportsPositiveDeltas() {
        OciMeterRegistry registry = createRegistry();
        AtomicLong state = new AtomicLong(11);
        OciFunctionalCounter<?> counter = (OciFunctionalCounter<?>) registry.getOrCreate(
                OciFunctionalCounter.builder("changed.in.flight", state, AtomicLong::get));

        assertThat(counter.deltaIfChanged(), is(OptionalLong.of(11L)));
        assertThat(counter.deltaIfChanged(), is(OptionalLong.empty()));

        state.set(15);

        assertThat(counter.deltaIfChanged(), is(OptionalLong.of(4L)));
        assertThat(counter.deltaIfChanged(), is(OptionalLong.empty()));
    }

    @Test
    void factoryProvidesDistributionSummaryBuilders() {
        MetricsConfig metricsConfig = metricsConfig();
        OciMetricsFactory factory = new OciMetricsFactory(delegateFactory(metricsConfig),
                                                          OciMetricsPublisherConfig.builder()
                                                                  .enabled(false)
                                                                  .reporterConfig(overlayReporterConfig())
                                                                  .defaultDimensions(Map.of())
                                                                  .buildPrototype(),
                                                          metricsConfig,
                                                          java.util.List.of());
        DistributionSummary.Builder builder = factory.distributionSummaryBuilder("payload",
                                                                                 factory.distributionStatisticsConfigBuilder());
        assertThat(builder.name(), is("payload"));
    }

    private static OciMeterRegistry createRegistry() {
        return createRegistry(Clock.system());
    }

    private static OciMeterRegistry createRegistry(Clock clock) {
        return createRegistry(clock,
                              OciMetricsPublisherConfig.builder()
                                      .enabled(true)
                                      .reporter(new TestMetricReporter())
                                      .reporterConfig(overlayReporterConfig())
                                      .defaultDimensions(Map.of())
                                      .buildPrototype());
    }

    private static OciMeterRegistry createRegistry(Clock clock, OciMetricsPublisherConfig publisherConfig) {
        MetricsConfig metricsConfig = metricsConfig();
        MetricsFactory delegateFactory = delegateFactory(metricsConfig);
        return new OciMeterRegistry(metricsConfig,
                                    clock,
                                    meter -> {
                                    },
                                    meter -> {
                                    },
                                    OciMetricsPublisher.create(publisherConfig),
                                    delegateFactory,
                                    delegateFactory.createMeterRegistry(clock, metricsConfig));
    }

    private static OciMetricsFactory createFactory(MetricsConfig metricsConfig) {
        return createFactory(metricsConfig,
                             OciMetricsPublisherConfig.builder()
                                     .enabled(false)
                                     .reporterConfig(overlayReporterConfig())
                                     .defaultDimensions(Map.of())
                                     .buildPrototype());
    }

    private static OciMetricsFactory createFactory(MetricsConfig metricsConfig, OciMetricsPublisherConfig publisherConfig) {
        return new OciMetricsFactory(delegateFactory(metricsConfig),
                                     publisherConfig,
                                     metricsConfig,
                                     java.util.List.of());
    }

    private static MetricsConfig metricsConfig() {
        return MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .build();
    }

    private static OverlayMetricReporterConfig overlayReporterConfig() {
        return OverlayMetricReporterConfig.builder()
                .project("proj")
                .fleet("fleet")
                .build();
    }

    private static MetricsFactory delegateFactory(MetricsConfig metricsConfig) {
        return new MicrometerMetricsFactoryProvider().create(Config.empty(), metricsConfig, java.util.List.of());
    }

    private static final class TestMetricReporter implements MetricReporter {
        @Override
        public void send(List<TimeSeries> timeSeries) {
        }

        @Override
        public void stop() {
        }
    }

    private static final class TestClock implements Clock {
        private long now;

        void advance(Duration duration) {
            now += duration.toNanos();
        }

        @Override
        public long wallTime() {
            return TimeUnit.NANOSECONDS.toMillis(now);
        }

        @Override
        public long monotonicTime() {
            return now;
        }
    }
}
