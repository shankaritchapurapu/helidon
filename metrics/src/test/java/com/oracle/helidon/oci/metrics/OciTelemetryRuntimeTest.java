/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Timer;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;
import io.helidon.service.registry.Services;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.model.TimeSeries;
import com.oracle.pic.telemetry.dianoga.MetricTimeSeriesClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OciTelemetryRuntimeTest {

    @Test
    void configuredHostNameWinsWithoutCallingResolver() {
        AtomicBoolean called = new AtomicBoolean();
        OverlayMetricReporterConfig config = overlayReporterConfigBuilder()
                .hostname("configured-host")
                .build();

        Optional<String> hostName = OciTelemetryRuntime.effectiveHostName(config, () -> {
            called.set(true);
            return "resolved-host";
        });

        assertThat(hostName, is(Optional.of("configured-host")));
        assertThat(called.get(), is(false));
    }

    @Test
    void missingHostNameUsesResolver() {
        OverlayMetricReporterConfig config = overlayReporterConfigBuilder().build();

        Optional<String> hostName = OciTelemetryRuntime.effectiveHostName(config, () -> "resolved-host");

        assertThat(hostName, is(Optional.of("resolved-host")));
    }

    @Test
    void missingHostNameContinuesWithoutHostDimensionWhenResolverFails() {
        CapturingLogHandler logHandler = CapturingLogHandler.attachTo(OciTelemetryRuntime.class);
        UnknownHostException failure = new UnknownHostException("test-host");
        try {
            OverlayMetricReporterConfig config = overlayReporterConfigBuilder().build();

            Optional<String> hostName = OciTelemetryRuntime.effectiveHostName(config, () -> {
                throw failure;
            });

            assertThat(hostName, is(Optional.empty()));
            assertThat(logHandler.warningThrown(), hasItem(is(failure)));
        } finally {
            logHandler.detach();
        }
    }

    @Test
    void closeDoesNotLogNullPointerExceptionWhenRuntimeWasNeverInitialized() {
        /*
        close() catches and logs Monitoring.close() failures so shutdown can continue, rather than throwing an exception.
        So capture the logging output to check for the presence or absence of a log message about the NPE.
         */
        CapturingLogHandler logHandler = CapturingLogHandler.attachTo(OciTelemetryRuntime.class);
        try {
            OciTelemetryRuntime runtime = new OciTelemetryRuntime(OciMetricsPublisherConfig.builder()
                                                                 .enabled(false)
                                                                 .defaultDimensions(Map.of())
                                                                 .buildPrototype());

            runtime.close();

            assertThat(logHandler.warningThrown(), not(hasItem(instanceOf(NullPointerException.class))));
        } finally {
            logHandler.detach();
        }
    }

    @Test
    void programmaticReporterStartsWithoutConfiguredProjectOrFleet() {
        shutdownMetricsIfActive();
        TestMetricReporter reporter = new TestMetricReporter();
        OciTelemetryRuntime runtime = new OciTelemetryRuntime(OciMetricsPublisherConfig.builder()
                                                              .reporter(reporter)
                                                              .defaultDimensions(Map.of())
                                                              .buildPrototype());
        try {
            runtime.publisher();

            assertThat(Metrics.isActive(), is(true));
        } finally {
            runtime.close();
            shutdownMetricsIfActive();
        }
    }

    @Test
    void factoryCloseSamplesPendingMetersBeforeShutdown() {
        shutdownMetricsIfActive();
        CapturingMetricReporter reporter = new CapturingMetricReporter();
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .build();
        OciMetricsFactory factory = new OciMetricsFactory(delegateFactory(metricsConfig),
                                                          OciMetricsPublisherConfig.builder()
                                                                  .reporter(reporter)
                                                                  .defaultDimensions(Map.of())
                                                                  .buildPrototype(),
                                                          metricsConfig,
                                                          List.of());
        try {
            MeterRegistry registry = factory.globalRegistry();
            Counter counter = registry.getOrCreate(Counter.builder("shutdown.counter"));

            counter.increment(7L);
            factory.close();

            assertThat(reporter.timeSeries()
                               .stream()
                               .map(series -> series.getMetricName().getName())
                               .toList(),
                       hasItem(equalTo("shutdown.counter")));
        } finally {
            shutdownMetricsIfActive();
        }
    }

    @Test
    void factoryCloseSamplesPendingMetersWhenMetricsWasAlreadyActive() {
        shutdownMetricsIfActive();
        CapturingMetricReporter activeReporter = new CapturingMetricReporter();
        Metrics.init(activeReporter, Map.of());
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .build();
        OciMetricsFactory factory = new OciMetricsFactory(delegateFactory(metricsConfig),
                                                          OciMetricsPublisherConfig.builder()
                                                                  .reporter(new CapturingMetricReporter())
                                                                  .defaultDimensions(Map.of())
                                                                  .buildPrototype(),
                                                          metricsConfig,
                                                          List.of());
        try {
            MeterRegistry registry = factory.globalRegistry();
            Counter counter = registry.getOrCreate(Counter.builder("preactive.shutdown.counter"));

            counter.increment(5L);
            factory.close();
        } finally {
            shutdownMetricsIfActive();
        }
        assertThat(activeReporter.timeSeries()
                                  .stream()
                                  .map(series -> series.getMetricName().getName())
                                  .toList(),
                   hasItem(equalTo("preactive.shutdown.counter")));
    }

    @Test
    void samplingFailureDoesNotInterruptShutdownCleanup() {
        shutdownMetricsIfActive();
        RuntimeException failure = new RuntimeException("expected sampling failure");
        CloseableMetricReporter reporter = new CloseableMetricReporter();
        CapturingLogHandler logHandler = CapturingLogHandler.attachTo(OciTelemetryRuntime.class);
        OciTelemetryRuntime runtime = new OciTelemetryRuntime(OciMetricsPublisherConfig.builder()
                                                              .reporter(reporter)
                                                              .defaultDimensions(Map.of())
                                                              .filter((name, meter) -> {
                                                                  if (OciMetricsPublisher.USER_AGENT_CARDINALITY_OVERFLOWS
                                                                          .equals(name)) {
                                                                      throw failure;
                                                                  }
                                                                  return true;
                                                              })
                                                              .buildPrototype());
        try {
            runtime.publisher().recordUserAgentCardinalityOverflow();

            runtime.close();

            assertThat(logHandler.warningThrown(), hasItem(is(failure)));
            assertThat(reporter.closed(), is(true));
            assertThat(Metrics.isActive(), is(false));
        } finally {
            runtime.close();
            logHandler.detach();
            shutdownMetricsIfActive();
        }
    }

    @Test
    void timerSamplesUseActiveMetricsRuntimeWhenMetricsWasAlreadyActive() {
        shutdownMetricsIfActive();
        CapturingMetricReporter activeReporter = new CapturingMetricReporter();
        CapturingMetricReporter configuredReporter = new CapturingMetricReporter();
        Metrics.init(activeReporter, Map.of());
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .build();
        OciMetricsFactory factory = new OciMetricsFactory(delegateFactory(metricsConfig),
                                                          OciMetricsPublisherConfig.builder()
                                                                  .reporter(configuredReporter)
                                                                  .defaultDimensions(Map.of())
                                                                  .buildPrototype(),
                                                          metricsConfig,
                                                          List.of());
        try {
            MeterRegistry registry = factory.globalRegistry();
            Timer timer = registry.getOrCreate(Timer.builder("direct.timer"));

            timer.record(Duration.ofMillis(12));
            factory.runtime().sampleMeters(true);
        } finally {
            factory.close();
            shutdownMetricsIfActive();
        }
        TimeSeries series = activeReporter.timeSeries()
                .stream()
                .filter(candidate -> candidate.getMetricName().getName().equals("direct.timer"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected direct.timer in " + activeReporter.timeSeries()));
        assertThat(series.getObservations().size(), is(1));
        assertThat(series.getObservations().getFirst().getValue(), is(12D));
        assertThat(configuredReporter.timeSeries()
                           .stream()
                           .map(candidate -> candidate.getMetricName().getName())
                           .toList(),
                   not(hasItem(equalTo("direct.timer"))));
    }

    @Test
    void excludedEventDrivenMetersDoNotEmitOriginalOrPressureTelemetry() {
        shutdownMetricsIfActive();
        CapturingMetricReporter reporter = new CapturingMetricReporter();
        MetricsConfig metricsConfig = metricsConfig();
        OciMetricsFactory factory = createFactory(metricsConfig,
                                                  OciMetricsPublisherConfig.builder()
                                                          .reporter(reporter)
                                                          .defaultDimensions(Map.of())
                                                          .sampleInterval(Duration.ofMinutes(1))
                                                          .excludes(java.util.Set.of("excluded.counter",
                                                                                    "excluded.timer",
                                                                                    "excluded.summary"))
                                                          .accumulators(accumulators -> accumulators
                                                                  .maxRawTimerSamplesPerSecond(2)
                                                                  .maxRawSummarySamplesPerSecond(2))
                                                          .buildPrototype());
        try {
            MeterRegistry registry = factory.globalRegistry();
            Counter counter = registry.getOrCreate(Counter.builder("excluded.counter"));
            Timer timer = registry.getOrCreate(Timer.builder("excluded.timer"));
            DistributionSummary summary = registry.getOrCreate(DistributionSummary.builder("excluded.summary"));

            counter.increment(3);
            timer.record(Duration.ofMillis(10));
            timer.record(Duration.ofMillis(20));
            timer.record(Duration.ofMillis(30));
            summary.record(10D);
            summary.record(20D);
            summary.record(30D);

            factory.runtime().sampleMeters(true);
        } finally {
            factory.close();
            shutdownMetricsIfActive();
        }

        assertThat(reporter.timeSeries(), empty());
    }

    @Test
    void samplingUsesRegistryClockToKeepCurrentSecondOpen() {
        shutdownMetricsIfActive();
        CapturingMetricReporter reporter = new CapturingMetricReporter();
        MetricsConfig metricsConfig = metricsConfig();
        OciMetricsFactory factory = createFactory(metricsConfig, reporter);
        TestClock clock = new TestClock();
        try {
            OciMeterRegistry registry = (OciMeterRegistry) factory.createMeterRegistry(clock, metricsConfig);
            OciTimer timer = (OciTimer) registry.getOrCreate(OciTimer.builder("clock.timer"));

            timer.record(Duration.ofMillis(12));
            factory.runtime().sampleMeters(false);

            assertThat(timer.pendingBucketCount(), is(1));

            clock.advance(Duration.ofSeconds(1));
            factory.runtime().sampleMeters(false);

            assertThat(timer.pendingBucketCount(), is(0));
        } finally {
            factory.close();
            shutdownMetricsIfActive();
        }
    }

    @Test
    void samplingUsesEachRegistryClockIndependently() {
        shutdownMetricsIfActive();
        CapturingMetricReporter reporter = new CapturingMetricReporter();
        MetricsConfig metricsConfig = metricsConfig();
        OciMetricsFactory factory = createFactory(metricsConfig, reporter);
        TestClock advancedClock = new TestClock();
        TestClock currentClock = new TestClock();
        try {
            OciMeterRegistry advancedRegistry =
                    (OciMeterRegistry) factory.createMeterRegistry(advancedClock, metricsConfig);
            OciMeterRegistry currentRegistry =
                    (OciMeterRegistry) factory.createMeterRegistry(currentClock, metricsConfig);
            OciTimer advancedTimer = (OciTimer) advancedRegistry.getOrCreate(OciTimer.builder("clock.advanced"));
            OciTimer currentTimer = (OciTimer) currentRegistry.getOrCreate(OciTimer.builder("clock.current"));

            advancedTimer.record(Duration.ofMillis(11));
            currentTimer.record(Duration.ofMillis(22));
            advancedClock.advance(Duration.ofSeconds(1));
            factory.runtime().sampleMeters(false);

            assertThat(advancedTimer.pendingBucketCount(), is(0));
            assertThat(currentTimer.pendingBucketCount(), is(1));
        } finally {
            factory.close();
            shutdownMetricsIfActive();
        }
    }

    @Test
    void overlayReporterDoesNotOwnProgrammaticClient() {
        TestMetricReporter reporter = new TestMetricReporter();
        TestCloseable closeable = new TestCloseable();

        MetricReporter result = OverlayMetricReporterFactory.applyOwnership(reporter, closeable, false);

        assertThat(result, is(reporter));
        assertThat(closeable.closed(), is(false));
    }

    @Test
    void overlayReporterClosesOwnedClient() throws Exception {
        TestMetricReporter reporter = new TestMetricReporter();
        TestCloseable closeable = new TestCloseable();

        MetricReporter result = OverlayMetricReporterFactory.applyOwnership(reporter, closeable, true);

        assertThat(result, instanceOf(OwnedMetricReporter.class));
        ((AutoCloseable) result).close();
        assertThat(closeable.closed(), is(true));
    }

    @Test
    void substrateReporterDoesNotOwnProgrammaticClient() {
        TestMetricReporter reporter = new TestMetricReporter();
        TestCloseable closeable = new TestCloseable();

        MetricReporter result = SubstrateMetricReporterFactory.applyOwnership(reporter, closeable, false);

        assertThat(result, is(reporter));
        assertThat(closeable.closed(), is(false));
    }

    @Test
    void substrateReporterClosesOwnedClient() throws Exception {
        TestMetricReporter reporter = new TestMetricReporter();
        TestCloseable closeable = new TestCloseable();

        MetricReporter result = SubstrateMetricReporterFactory.applyOwnership(reporter, closeable, true);

        assertThat(result, instanceOf(OwnedMetricReporter.class));
        ((AutoCloseable) result).close();
        assertThat(closeable.closed(), is(true));
    }

    @Test
    void substrateReporterUsesRegistryClientWhenProgrammaticClientAbsent() throws Exception {
        MetricTimeSeriesClient registryClient = metricTimeSeriesClient();
        Services.set(MetricTimeSeriesClient.class, registryClient);
        MetricReporter reporter = SubstrateMetricReporterFactory.create(SubstrateMetricReporterConfig.builder()
                                                                                .project("test-project")
                                                                                .fleet("test-fleet")
                                                                                .build());

        assertThat(reporter, instanceOf(OwnedMetricReporter.class));
        ((AutoCloseable) reporter).close();
        verify(registryClient).close();
    }

    @Test
    void substrateReporterDoesNotOwnProgrammaticClientFromCreate() {
        MetricTimeSeriesClient programmaticClient = metricTimeSeriesClient();
        MetricReporter reporter = SubstrateMetricReporterFactory.create(SubstrateMetricReporterConfig.builder()
                                                                                .project("test-project")
                                                                                .fleet("test-fleet")
                                                                                .metricTimeSeriesClient(programmaticClient)
                                                                                .build());

        assertThat(reporter, not(instanceOf(OwnedMetricReporter.class)));
    }

    private static OverlayMetricReporterConfig.Builder overlayReporterConfigBuilder() {
        return OverlayMetricReporterConfig.builder()
                .project("test-project")
                .fleet("test-fleet");
    }

    private static MetricTimeSeriesClient metricTimeSeriesClient() {
        return mock(MetricTimeSeriesClient.class);
    }

    private static MetricsConfig metricsConfig() {
        return MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .build();
    }

    private static OciMetricsFactory createFactory(MetricsConfig metricsConfig, MetricReporter reporter) {
        return createFactory(metricsConfig,
                             OciMetricsPublisherConfig.builder()
                                     .reporter(reporter)
                                     .defaultDimensions(Map.of())
                                     .sampleInterval(Duration.ofMinutes(1))
                                     .buildPrototype());
    }

    private static OciMetricsFactory createFactory(MetricsConfig metricsConfig, OciMetricsPublisherConfig publisherConfig) {
        return new OciMetricsFactory(delegateFactory(metricsConfig),
                                     publisherConfig,
                                     metricsConfig,
                                     List.of());
    }

    private static void shutdownMetricsIfActive() {
        if (Metrics.isActive()) {
            Metrics.shutdown();
        }
    }

    private static MetricsFactory delegateFactory(MetricsConfig metricsConfig) {
        return new MicrometerMetricsFactoryProvider().create(Config.empty(), metricsConfig, List.of());
    }

    private static final class TestMetricReporter implements MetricReporter {
        @Override
        public void send(List<TimeSeries> timeSeries) {
        }

        @Override
        public void stop() {
        }
    }

    private static final class CapturingMetricReporter implements MetricReporter {
        private final List<TimeSeries> timeSeries = new CopyOnWriteArrayList<>();

        @Override
        public void send(List<TimeSeries> timeSeries) {
            this.timeSeries.addAll(timeSeries);
        }

        @Override
        public void stop() {
        }

        private List<TimeSeries> timeSeries() {
            return List.copyOf(timeSeries);
        }
    }

    private static final class CloseableMetricReporter implements MetricReporter, AutoCloseable {
        private volatile boolean closed;

        @Override
        public void send(List<TimeSeries> timeSeries) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void close() {
            closed = true;
        }

        boolean closed() {
            return closed;
        }
    }

    private static final class TestCloseable implements AutoCloseable {
        private volatile boolean closed;

        @Override
        public void close() {
            closed = true;
        }

        boolean closed() {
            return closed;
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

    private static final class CapturingLogHandler extends Handler {
        private final Logger logger;
        private final Level originalLevel;
        private final boolean originalUseParentHandlers;
        private final List<LogRecord> records = new CopyOnWriteArrayList<>();

        private CapturingLogHandler(Logger logger) {
            this.logger = logger;
            this.originalLevel = logger.getLevel();
            this.originalUseParentHandlers = logger.getUseParentHandlers();
        }

        static CapturingLogHandler attachTo(Class<?> type) {
            Logger logger = Logger.getLogger(type.getName());
            CapturingLogHandler handler = new CapturingLogHandler(logger);
            handler.setLevel(Level.ALL);
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
            return handler;
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        void detach() {
            logger.removeHandler(this);
            logger.setLevel(originalLevel);
            logger.setUseParentHandlers(originalUseParentHandlers);
        }

        List<Throwable> warningThrown() {
            return records.stream()
                    .filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
                    .map(LogRecord::getThrown)
                    .toList();
        }
    }
}
