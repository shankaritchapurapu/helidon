/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import com.oracle.pic.telemetry.commons.metrics.model.Observation;

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
    private static final Duration SHUTDOWN_FLUSH_TIMEOUT = Duration.ofSeconds(1);

    private final OciMetricsPublisherConfig config;
    private final OciMetricsPublisher publisher;
    private final List<OciMeterRegistry> registries = new CopyOnWriteArrayList<>();
    private final Map<OciGauge<?>, OciGauge.Sample> pendingGaugeSamples = new IdentityHashMap<>();
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

    /**
     * Removes a registry from the sampling set when an OCI wrapper is closed.
     */
    void unregisterRegistry(OciMeterRegistry registry) {
        registries.remove(registry);
        synchronized (pendingGaugeSamples) {
            pendingGaugeSamples.keySet().removeIf(gauge -> gauge.registry() == registry);
        }
        LOGGER.log(System.Logger.Level.TRACE,
                   "Unregistered Helidon Talon meter registry; registries={0}",
                   registries.size());
    }

    /**
     * Clears factory-owned registry tracking during factory close so no closed registry remains in the sampler list.
     */
    void clearRegistries() {
        registries.clear();
        synchronized (pendingGaugeSamples) {
            pendingGaugeSamples.clear();
        }
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
                awaitSamplerTermination(executor);
            }
            if (Metrics.isActive()) {
                flushPendingMeterData();
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

    private void awaitSamplerTermination(ScheduledExecutorService executor) {
        try {
            if (!executor.awaitTermination(SHUTDOWN_FLUSH_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                LOGGER.log(System.Logger.Level.WARNING,
                           "Timed out waiting for Helidon Talon metrics sampler to stop before shutdown flush.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(System.Logger.Level.WARNING,
                       "Interrupted waiting for Helidon Talon metrics sampler to stop before shutdown flush.",
                       e);
        }
    }

    private void flushPendingMeterData() {
        if (!OciMetricsSemanticConventions.awaitAsyncUpdates(SHUTDOWN_FLUSH_TIMEOUT)) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Timed out waiting for automatic HTTP metrics updates to finish before shutdown flush.");
        }
        LOGGER.log(System.Logger.Level.TRACE, "Sampling Helidon Talon metrics during shutdown flush");
        sampleMetersSafely(true);
    }

    /**
     * {@link ScheduledExecutorService} suppresses future fixed-rate executions if a task throws. Contains sampling failures
     * so periodic sampling continues and a failed shutdown flush does not prevent the remaining resources from being closed.
     */
    private void sampleMetersSafely(boolean includeCurrentSecond) {
        try {
            sampleMeters(includeCurrentSecond);
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Unexpected error sampling Helidon Talon metrics; continuing runtime operation.",
                       e);
        }
    }

    /*
    Package-private so integration test can trigger sampling deterministically.
     */
    void sampleMeters() {
        sampleMeters(false);
    }

    void sampleMeters(boolean includeCurrentSecond) {
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Sampling known meters across registries={0}",
                       registries.size());
        }
        long pendingBuckets = 0L;
        Set<OciGauge<?>> liveGauges = Collections.newSetFromMap(new IdentityHashMap<>());
        for (OciMeterRegistry registry : registries) {
            long nowMillis = registry.clock().wallTime();
            for (var meter : registry.meters()) {
                switch (meter) {
                case OciGauge<?> gauge -> {
                    liveGauges.add(gauge);
                    sampleGauge(gauge);
                }
                case OciFunctionalCounter<?> functionalCounter -> sampleFunctionalCounter(functionalCounter);
                case OciCounter counter -> sampleCounter(counter);
                case OciTimer timer -> {
                    sampleTimer(timer, nowMillis, includeCurrentSecond);
                    pendingBuckets += timer.pendingBucketCount();
                }
                case OciDistributionSummary summary -> {
                    sampleDistributionSummary(summary, nowMillis, includeCurrentSecond);
                    pendingBuckets += summary.pendingBucketCount();
                }
                default -> {
                }
                }
            }
        }
        synchronized (pendingGaugeSamples) {
            pendingGaugeSamples.keySet().removeIf(gauge -> !liveGauges.contains(gauge));
        }
        publisher.publishAccumulatorPressure(pendingBuckets);
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
        if (Metrics.isActive()) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Helidon Talon metrics runtime found OCI Metrics object already initialized; Helidon Talon metrics config "
                               + "was not applied.");
        } else {
            if (configuredReporter instanceof DeferredMetricReporter
                    && (reporterConfig.project().isEmpty() || reporterConfig.fleet().isEmpty())) {
                LOGGER.log(System.Logger.Level.WARNING,
                           "OCI metrics provider is enabled but project/fleet are not fully configured; publishing is disabled.");
                publisher.stop();
                return;
            }

            MetricReporter initializedReporter = reporter(configuredReporter);
            LOGGER.log(System.Logger.Level.TRACE,
                       "Initializing OCI telemetry runtime; project={0}, fleet={1}, reporterType={2}",
                       reporterConfig.project().orElse(""),
                       reporterConfig.fleet().orElse(""),
                       reporterConfig.type());
            Metrics.init(initializedReporter, config.defaultDimensions());
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

        long intervalMillis = config.sampleInterval().toMillis();
        LOGGER.log(System.Logger.Level.TRACE,
                   "Scheduling metrics sampling; intervalMillis={0}, registries={1}",
                   intervalMillis,
                   registries.size());
        try {
            executor.scheduleAtFixedRate(() -> sampleMetersSafely(false),
                                         intervalMillis,
                                         intervalMillis,
                                         TimeUnit.MILLISECONDS);
            samplingExecutor = executor;
        } catch (RuntimeException e) {
            executor.shutdownNow();
            throw e;
        }
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
        synchronized (pendingGaugeSamples) {
            OciGauge.Sample pending = pendingGaugeSamples.get(gauge);
            OciGauge.Sample inFlight = pending;
            try {
                if (pending != null) {
                    publisher.publishGauge(gauge, pending);
                    pendingGaugeSamples.remove(gauge);
                }
                inFlight = gauge.sample(System.currentTimeMillis());
                publisher.publishGauge(gauge, inFlight);
            } catch (RuntimeException e) {
                if (inFlight != null) {
                    pendingGaugeSamples.put(gauge, inFlight);
                }
                LOGGER.log(System.Logger.Level.WARNING,
                           "Error sampling OCI-backed gauge " + gauge.id().name()
                                   + "; publishing will be retried in the next sampling interval.",
                           e);
            }
        }
    }

    private void sampleFunctionalCounter(OciFunctionalCounter<?> functionalCounter) {
        if (!functionalCounter.enabled()) {
            return;
        }
        functionalCounter.lockSampling();
        try {
            /*
            Hold the meter's sampling lock across drain/publish/restore. Retrying a failed restore CAS after
            another sampler has published newer state could duplicate deltas.
             */
            OciFunctionalCounter.Sample sample = functionalCounter.drainDeltaIfChanged();
            try {
                sample.positiveDelta()
                        .ifPresent(delta -> publisher.publishFunctionalCounterDelta(functionalCounter, delta));
            } catch (RuntimeException e) {
                functionalCounter.restoreDelta(sample);
                LOGGER.log(System.Logger.Level.WARNING,
                           "Error sampling OCI-backed functional counter " + functionalCounter.id().name()
                                   + "; publishing will be retried in the next sampling interval.",
                           e);
            }
        } finally {
            functionalCounter.unlockSampling();
        }
    }

    private void sampleCounter(OciCounter counter) {
        if (!counter.enabled()) {
            return;
        }
        long delta = counter.drainDelta();
        try {
            publisher.publishCounterDelta(counter, delta);
        } catch (RuntimeException e) {
            counter.restoreDelta(delta);
            LOGGER.log(System.Logger.Level.WARNING,
                       "Error sampling OCI-backed counter " + counter.id().name()
                               + "; publishing will be retried in the next sampling interval.",
                       e);
        }
    }

    private void sampleTimer(OciTimer timer, long nowMillis, boolean includeCurrentSecond) {
        if (!timer.enabled()) {
            return;
        }
        List<Observation> observations =
                includeCurrentSecond ? timer.drainAllIntervalSamples() : timer.drainClosedIntervalSamples(nowMillis);
        try {
            publisher.publishObservations(timer, observations, "timer duration samples");
        } catch (RuntimeException e) {
            timer.restoreIntervalSamples(observations);
            LOGGER.log(System.Logger.Level.WARNING,
                       "Error sampling OCI-backed timer " + timer.id().name()
                               + "; publishing will be retried in the next sampling interval.",
                       e);
        }
    }

    private void sampleDistributionSummary(OciDistributionSummary summary, long nowMillis, boolean includeCurrentSecond) {
        if (!summary.enabled()) {
            return;
        }
        List<Observation> observations =
                includeCurrentSecond ? summary.drainAllIntervalSamples() : summary.drainClosedIntervalSamples(nowMillis);
        try {
            publisher.publishObservations(summary, observations, "distribution summary samples");
        } catch (RuntimeException e) {
            summary.restoreIntervalSamples(observations);
            LOGGER.log(System.Logger.Level.WARNING,
                       "Error sampling OCI-backed distribution summary " + summary.id().name()
                               + "; publishing will be retried in the next sampling interval.",
                       e);
        }
    }
}
