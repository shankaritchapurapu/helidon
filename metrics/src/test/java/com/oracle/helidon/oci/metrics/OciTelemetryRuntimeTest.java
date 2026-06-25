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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class OciTelemetryRuntimeTest {

    @Test
    void configuredHostNameWinsWithoutCallingResolver() {
        AtomicBoolean called = new AtomicBoolean();
        OciMetricsPublisherConfig config = publisherConfigBuilder()
                .hostname("configured-host")
                .buildPrototype();

        Optional<String> hostName = OciTelemetryRuntime.effectiveHostName(config, () -> {
            called.set(true);
            return "resolved-host";
        });

        assertThat(hostName, is(Optional.of("configured-host")));
        assertThat(called.get(), is(false));
    }

    @Test
    void missingHostNameUsesResolver() {
        OciMetricsPublisherConfig config = publisherConfigBuilder().buildPrototype();

        Optional<String> hostName = OciTelemetryRuntime.effectiveHostName(config, () -> "resolved-host");

        assertThat(hostName, is(Optional.of("resolved-host")));
    }

    @Test
    void missingHostNameContinuesWithoutHostDimensionWhenResolverFails() {
        CapturingLogHandler logHandler = CapturingLogHandler.attachTo(OciTelemetryRuntime.class);
        UnknownHostException failure = new UnknownHostException("test-host");
        try {
            OciMetricsPublisherConfig config = publisherConfigBuilder().buildPrototype();

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
                                                                 .requestHeaders(Map.of())
                                                                 .buildPrototype());

            runtime.close();

            assertThat(logHandler.warningThrown(), not(hasItem(instanceOf(NullPointerException.class))));
        } finally {
            logHandler.detach();
        }
    }

    private static OciMetricsPublisherConfig.Builder publisherConfigBuilder() {
        return OciMetricsPublisherConfig.builder()
                .enabled(true)
                .project("test-project")
                .fleet("test-fleet")
                .defaultDimensions(Map.of())
                .requestHeaders(Map.of());
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
