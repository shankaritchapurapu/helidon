/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import io.helidon.service.registry.Services;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.model.TimeSeries;
import com.oracle.pic.telemetry.dianoga.MetricTimeSeriesClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
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
        if (Metrics.isActive()) {
            Metrics.shutdown();
        }
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
            if (Metrics.isActive()) {
                Metrics.shutdown();
            }
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

    private static final class TestMetricReporter implements MetricReporter {
        @Override
        public void send(List<TimeSeries> timeSeries) {
        }

        @Override
        public void stop() {
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
