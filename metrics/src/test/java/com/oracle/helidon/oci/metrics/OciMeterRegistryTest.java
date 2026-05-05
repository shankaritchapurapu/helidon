/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

class OciMeterRegistryTest {

    @EnabledIfSystemProperty(named = "helidon.oci.metrics.tests.local", matches = "true")
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

    @EnabledIfSystemProperty(named = "helidon.oci.metrics.tests.local", matches = "true")
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

    @EnabledIfSystemProperty(named = "helidon.oci.metrics.tests.local", matches = "true")
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
        return (OciMeterRegistry) createFactory().createMeterRegistry(Clock.system(), metricsConfig());
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
}
