/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.helidon.config.Config;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Timer;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class HelidonDelegateMetersTest {

    @Test
    void micrometerDelegateMetersTrackLocalState() {
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(true)
                .build();
        MetricsFactory factory = new MicrometerMetricsFactoryProvider().create(Config.empty(), metricsConfig, List.of());
        MeterRegistry registry = factory.createMeterRegistry(metricsConfig);
        try {
            Counter counter = registry.getOrCreate(factory.counterBuilder("delegate.requests"));
            counter.increment(3);

            assertThat(counter.count(), is(3L));

            Timer timer = registry.getOrCreate(factory.timerBuilder("delegate.latency"));
            timer.record(Duration.ofMillis(10));
            timer.record(20, TimeUnit.MILLISECONDS);

            assertThat(timer.count(), is(2L));
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS), is(30D));
            assertThat(timer.mean(TimeUnit.MILLISECONDS), is(15D));
            assertThat(timer.max(TimeUnit.MILLISECONDS), is(20D));

            DistributionSummary summary = registry.getOrCreate(factory.distributionSummaryBuilder(
                    "delegate.payload",
                    factory.distributionStatisticsConfigBuilder()));
            summary.record(10D);
            summary.record(20D);

            assertThat(summary.count(), is(2L));
            assertThat(summary.totalAmount(), is(30D));
            assertThat(summary.mean(), is(15D));
            assertThat(summary.max(), is(20D));
        } finally {
            registry.close();
            factory.close();
        }
    }
}
