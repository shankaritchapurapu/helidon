/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.Main;
import io.helidon.spi.HelidonShutdownHandler;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.Metrics;

/**
 * OCI Telemetry Runtime which oversees the Helidon Talon metrics implementation, primarily:
 * <ul>
 *     <li>preparing connectivity to the backend ({@code Monitoring} and {@code TelemetryReporterBuilder}) and the
 *     OCI metrics {@code Metrics} object,</li>
 *     <li>maintaining the list of registries (typically just one), and</li>
 *     <li>managing the executor that periodically samples meters.</li>
 * </ul>
 * We sample all supported meters periodically so changes are not reported to OCI from application mutation paths.
 * <p>
 * The OCI metrics library itself does internal buffering, batching, and transmission of metrics data to the backend, so this
 * component does not need to do that.
 */
final class OciTelemetryRuntime implements AutoCloseable, HelidonShutdownHandler {

    /*
    Most TRACE-level logging in this class is during start-up or shutdown so is not guarded with isLoggable.
     */
    private static final System.Logger LOGGER = System.getLogger(OciTelemetryRuntime.class.getName());

    private final OciMetricsPublisherConfig config;
    private final OciMetricsPublisher publisher;
    private final List<OciMeterRegistry> registries = new CopyOnWriteArrayList<>();
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile ScheduledExecutorService samplingExecutor;
    private volatile boolean initializedMetrics;

    OciTelemetryRuntime(OciMetricsPublisherConfig config) {
        this.config = config;
        this.publisher = OciMetricsPublisher.create(config);
    }

    OciMetricsPublisher publisher() {
        start();
        return publisher;
    }

    void registerRegistry(OciMeterRegistry registry) {
        registries.add(registry);
        LOGGER.log(System.Logger.Level.TRACE,
                   "Registered Helidon Talon meter registry; registries={0}, meters={1}",
                   registries.size(),
                   registry.meters().size());
        start();
    }

    void start() {
        lifecycleLock.lock();
        try {
            if (closed.get() || started.get()) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Skipping Helidon Talon telemetry runtime start; closed={0}, started={1}",
                           closed.get(),
                           started.get());
                return;
            }
            LOGGER.log(System.Logger.Level.TRACE, "Starting Helidon Talon telemetry runtime");
            initializeRuntime();
            Main.addShutdownHandler(this);
            started.set(true);
            LOGGER.log(System.Logger.Level.TRACE, "Helidon Talon telemetry runtime started");
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Override
    public void shutdown() {
        close();
    }

    @Override
    public void close() {
        lifecycleLock.lock();
        try {
            if (!closed.compareAndSet(false, true)) {
                LOGGER.log(System.Logger.Level.TRACE, "Skipping Helidon Talon telemetry runtime close; already closed");
                return;
            }
            LOGGER.log(System.Logger.Level.TRACE, "Closing Helidon Talon telemetry runtime");
            ScheduledExecutorService executor = samplingExecutor;
            samplingExecutor = null;
            if (executor != null) {
                LOGGER.log(System.Logger.Level.TRACE, "Stopping Helidon Talon metrics sampling executor");
                executor.shutdownNow();
            }
            publisher.stop();
            if (initializedMetrics && Metrics.isActive()) {
                LOGGER.log(System.Logger.Level.TRACE, "Shutting down OCI Metrics");
                Metrics.shutdown();
            }
            MetricReporter reporter = config.reporter();
            if (reporter instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    LOGGER.log(System.Logger.Level.WARNING, "Error closing OCI metrics reporter", e);
                }
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    /*
    Package-private so integration test can trigger sampling deterministically.
     */
    void sampleMeters() {
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Sampling known meters across registries={0}",
                       registries.size());
        }
        registries.forEach(registry -> {
            registry.gauges().forEach(this::sampleGauge);
            registry.functionalCounters().forEach(this::sampleFunctionalCounter);
            registry.counters().forEach(this::sampleCounter);
            registry.timers().forEach(this::sampleTimer);
            registry.distributionSummaries().forEach(this::sampleDistributionSummary);
        });
    }

    private void initializeRuntime() {
        if (closed.get()) {
            LOGGER.log(System.Logger.Level.TRACE, "Runtime initialization aborted because runtime is closed");
            publisher.stop();
            return;
        }
        if (!config.enabled()) {
            LOGGER.log(System.Logger.Level.TRACE, "Runtime initialization skipped because publisher is disabled");
            publisher.stop();
            return;
        }
        /*
        The reporterConfig will be either for overlay or substrate, according to the overall OCI metrics publisher config, and
        therefore so will be reporter itself be either for overlay or substrate.
         */
        OciMetricReporterConfig reporterConfig = config.reporterConfig();
        MetricReporter configuredReporter = config.reporter();
        if (configuredReporter instanceof DeferredMetricReporter
                && (reporterConfig.project().isEmpty() || reporterConfig.fleet().isEmpty())) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "OCI metrics provider is enabled but project/fleet are not fully configured; publishing is disabled.");
            publisher.stop();
            return;
        }

        if (Metrics.isActive()) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Helidon Talon metrics runtime found OCI Metrics object already initialized; Helidon Talon metrics config "
                               + "was not applied.");
        } else {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Initializing OCI telemetry runtime; project={0}, fleet={1}, reporterType={2}",
                       reporterConfig.project().orElse(""),
                       reporterConfig.fleet().orElse(""),
                       reporterConfig.type());
            Metrics.init(reporter(configuredReporter), config.defaultDimensions());
            initializedMetrics = true;
            LOGGER.log(System.Logger.Level.TRACE,
                       "Initialized telemetry Metrics runtime; defaultDimensions={0}",
                       config.defaultDimensions());
        }
        scheduleSampling();
    }

    private static MetricReporter reporter(MetricReporter reporter) {
        if (reporter instanceof DeferredMetricReporter deferredReporter) {
            return deferredReporter.initialize();
        }
        return reporter;
    }

    private void scheduleSampling() {
        if (samplingExecutor != null) {
            LOGGER.log(System.Logger.Level.TRACE, "Skipping metrics sampling schedule; executor already exists");
            return;
        }
        var executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
                                                                          .name("Helidon Talon metrics sampler")
                                                                          .factory());

        samplingExecutor = executor;
        long intervalMillis = Math.max(1000L, config.sampleInterval().toMillis());
        LOGGER.log(System.Logger.Level.TRACE,
                   "Scheduling metrics sampling; intervalMillis={0}, registries={1}",
                   intervalMillis,
                   registries.size());
        executor.scheduleAtFixedRate(this::sampleMeters,
                                     intervalMillis,
                                     intervalMillis,
                                     TimeUnit.MILLISECONDS);
    }

    static Optional<String> effectiveHostName(OciMetricReporterConfig config) {
        return effectiveHostName(config.hostname(), () -> InetAddress.getLocalHost().getHostName());
    }

    static Optional<String> effectiveHostName(OciMetricReporterConfig config, Callable<String> resolver) {
        return effectiveHostName(config.hostname(), resolver);
    }

    private static Optional<String> effectiveHostName(Optional<String> configuredHostName, Callable<String> resolver) {
        if (configuredHostName.isPresent()) {
            return configuredHostName;
        }
        try {
            return Optional.of(resolver.call());
        } catch (UnknownHostException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Hostname is not configured explicitly and local host name cannot be resolved; "
                               + "continuing without the host metrics dimension.",
                       e);
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error resolving local host name for metrics", e);
        }
    }

    private void sampleGauge(OciGauge<?> gauge) {
        if (!gauge.enabled()) {
            return;
        }
        try {
            gauge.valueIfChanged().ifPresent(value -> publisher.publishGauge(gauge, value.doubleValue()));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Error sampling OCI-backed gauge " + gauge.id().name(), e);
        }
    }

    private void sampleFunctionalCounter(OciFunctionalCounter<?> functionalCounter) {
        if (!functionalCounter.enabled()) {
            return;
        }
        try {
            functionalCounter.valueIfChanged()
                    .ifPresent(value -> publisher.publishFunctionalCounter(functionalCounter, value));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Error sampling OCI-backed functional counter " + functionalCounter.id().name(),
                       e);
        }
    }

    private void sampleCounter(OciCounter counter) {
        if (!counter.enabled()) {
            return;
        }
        try {
            counter.deltaIfChanged().ifPresent(delta -> publisher.publishCounterDelta(counter, delta));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Error sampling OCI-backed counter " + counter.id().name(), e);
        }
    }

    private void sampleTimer(OciTimer timer) {
        if (!timer.enabled()) {
            return;
        }
        try {
            publisher.publishTimerRate(timer, timer.oneMinuteRate());
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Error sampling OCI-backed timer " + timer.id().name(), e);
        }
    }

    private void sampleDistributionSummary(OciDistributionSummary summary) {
        if (!summary.enabled()) {
            return;
        }
        try {
            summary.intervalMeanIfChanged()
                    .ifPresent(value -> publisher.publishDistributionSummaryMean(summary, value));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Error sampling OCI-backed distribution summary " + summary.id().name(),
                       e);
        }
    }
}
