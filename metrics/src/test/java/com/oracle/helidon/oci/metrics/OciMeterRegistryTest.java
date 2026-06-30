/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.helidon.config.Config;
import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionStatisticsConfig;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.FunctionalCounter;
import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Timer;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

class OciMeterRegistryTest {

    @Test
    void counterMaintainsCountAndReusesRegistration() {
        OciMeterRegistry registry = createRegistry();

        Counter first = registry.getOrCreate(OciCounter.builder("requests")
                                                     .addTag(io.helidon.metrics.api.Tag.create("k", "v")));
        Counter second = registry.getOrCreate(OciCounter.builder("requests")
                                                      .addTag(io.helidon.metrics.api.Tag.create("k", "v")));

        first.increment(3);

        assertThat(second, sameInstance(first));
        assertThat(first.count(), is(3L));
    }

    @Test
    void counterSamplesOnlyPositiveDeltas() {
        OciMeterRegistry registry = createRegistry();
        OciCounter counter = (OciCounter) registry.getOrCreate(OciCounter.builder("sampled.requests"));

        counter.increment(3);

        assertThat(counter.deltaIfChanged(), is(OptionalLong.of(3L)));
        assertThat(counter.deltaIfChanged(), is(OptionalLong.empty()));

        counter.increment(2);

        assertThat(counter.deltaIfChanged(), is(OptionalLong.of(2L)));
    }

    @Test
    void counterResetUpdatesSamplingBaselineWithoutPublishingNegativeDelta() throws Exception {
        OciMeterRegistry registry = createRegistry();
        OciCounter counter = (OciCounter) registry.getOrCreate(OciCounter.builder("reset.requests"));

        setLastSampledCount(counter, 10L);

        assertThat(counter.deltaIfChanged(), is(OptionalLong.empty()));

        counter.increment(2);

        assertThat(counter.deltaIfChanged(), is(OptionalLong.of(2L)));
    }

    @Test
    void gaugeSamplesOnAccessWithoutCaching() {
        OciMeterRegistry registry = createRegistry();
        AtomicLong value = new AtomicLong(5);
        Gauge<Long> gauge = registry.getOrCreate(OciGauge.builder("queue.size", value::get));

        assertThat(gauge.value(), is(5L));
        value.set(7);
        assertThat(gauge.value(), is(7L));
    }

    @Test
    void gaugeReportsOnlyChangedSampleValues() {
        OciMeterRegistry registry = createRegistry();
        AtomicLong value = new AtomicLong(5);
        OciGauge<?> gauge = (OciGauge<?>) registry.getOrCreate(OciGauge.builder("changed.queue.size", value::get));

        assertThat(gauge.valueIfChanged(), is(Optional.of(5L)));
        assertThat(gauge.valueIfChanged(), is(Optional.empty()));

        value.set(7);

        assertThat(gauge.valueIfChanged(), is(Optional.of(7L)));
        assertThat(gauge.valueIfChanged(), is(Optional.empty()));
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
    void timerSamplesDropwizardCompatibleOneMinuteRate() {
        TestClock clock = new TestClock();
        OciMeterRegistry registry = createRegistry(clock);
        OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("sampled.latency"));

        timer.record(Duration.ofMillis(10));
        timer.record(20, TimeUnit.MILLISECONDS);

        assertThat(timer.oneMinuteRate(), is(0D));

        clock.advance(Duration.ofSeconds(5).plusNanos(1));

        assertThat(timer.oneMinuteRate(), closeTo(0.4D, 0.000001D));
    }

    @Test
    void oneMinuteRateDecaysAfterEmptyTicks() {
        TestClock clock = new TestClock();
        OciOneMinuteRate rate = new OciOneMinuteRate(clock);

        rate.mark();
        clock.advance(Duration.ofSeconds(5).plusNanos(1));
        assertThat(rate.rate(), closeTo(0.2D, 0.000001D));

        clock.advance(Duration.ofSeconds(5));

        assertThat(rate.rate(), closeTo(0.2D + (1 - Math.exp(-5.0 / 60.0)) * (0D - 0.2D), 0.000001D));
    }

    @Test
    void oneMinuteRateAppliesMultipleElapsedTicks() {
        TestClock clock = new TestClock();
        OciOneMinuteRate rate = new OciOneMinuteRate(clock);

        rate.mark();
        clock.advance(Duration.ofSeconds(15).plusNanos(1));

        double alpha = 1 - Math.exp(-5.0 / 60.0);
        double expected = 0.2D;
        expected += alpha * (0D - expected);
        expected += alpha * (0D - expected);

        assertThat(rate.rate(), closeTo(expected, 0.000001D));
    }

    @Test
    void distributionSummaryTracksCountTotalMeanAndMax() {
        OciMeterRegistry registry = createRegistry();
        DistributionSummary summary = registry.getOrCreate(OciDistributionSummary.builder("payload",
                                                                                          DistributionStatisticsConfig.builder()));

        summary.record(10D);
        summary.record(20D);

        assertThat(summary.count(), is(2L));
        assertThat(summary.totalAmount(), is(30D));
        assertThat(summary.mean(), is(15D));
        assertThat(summary.max(), is(20D));
        assertThat(summary.snapshot(), notNullValue());
    }

    @Test
    void distributionSummarySamplesIntervalMeanOnlyWhenCountChanges() {
        OciMeterRegistry registry = createRegistry();
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("sampled.payload", DistributionStatisticsConfig.builder()));

        summary.record(10D);
        summary.record(20D);

        assertThat(summary.intervalMeanIfChanged(), is(OptionalDouble.of(15D)));
        assertThat(summary.intervalMeanIfChanged(), is(OptionalDouble.empty()));

        summary.record(5D);

        assertThat(summary.intervalMeanIfChanged(), is(OptionalDouble.of(5D)));
    }

    @Test
    void distributionSummarySamplesNormalizedIntervalMean() {
        OciMeterRegistry registry = createRegistry();
        OciDistributionSummary summary = (OciDistributionSummary) registry.getOrCreate(
                OciDistributionSummary.builder("sampled.kilobytes")
                        .baseUnit("kilobytes"));

        summary.record(1D);
        summary.record(2D);

        assertThat(summary.intervalMeanIfChanged(), is(OptionalDouble.of(1536D)));
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
    void functionalCounterReportsOnlyChangedSampleValues() {
        OciMeterRegistry registry = createRegistry();
        AtomicLong state = new AtomicLong(11);
        OciFunctionalCounter<?> counter = (OciFunctionalCounter<?>) registry.getOrCreate(
                OciFunctionalCounter.builder("changed.in.flight", state, AtomicLong::get));

        assertThat(counter.valueIfChanged(), is(Optional.of(11L)));
        assertThat(counter.valueIfChanged(), is(Optional.empty()));

        state.set(15);

        assertThat(counter.valueIfChanged(), is(Optional.of(15L)));
        assertThat(counter.valueIfChanged(), is(Optional.empty()));
    }

    @Test
    void factoryProvidesDistributionSummaryBuilders() {
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
        DistributionSummary.Builder builder = factory.distributionSummaryBuilder("payload",
                                                                                 factory.distributionStatisticsConfigBuilder());
        assertThat(builder.name(), is("payload"));
    }

    private static OciMeterRegistry createRegistry() {
        return createRegistry(Clock.system());
    }

    private static OciMeterRegistry createRegistry(Clock clock) {
        return (OciMeterRegistry) createFactory().createMeterRegistry(clock, metricsConfig());
    }

    private static OciMetricsFactory createFactory() {
        MetricsConfig metricsConfig = metricsConfig();
        return new OciMetricsFactory(delegateFactory(metricsConfig),
                                     OciMetricsPublisherConfig.builder()
                                             .enabled(false)
                                             .project("proj")
                                             .fleet("fleet")
                                             .defaultDimensions(Map.of())
                                             .requestHeaders(Map.of())
                                             .buildPrototype(),
                                     metricsConfig,
                                     java.util.List.of());
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

    private static void setLastSampledCount(OciCounter counter, long value) throws Exception {
        Field field = OciCounter.class.getDeclaredField("lastSampledCount");
        field.setAccessible(true);
        ((AtomicLong) field.get(counter)).set(value);
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
