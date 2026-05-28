/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.Main;
import io.helidon.service.registry.Services;
import io.helidon.spi.HelidonShutdownHandler;

import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporter;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporterBuilder;

/**
 * OCI Telemetry Runtime which oversees the Helidon OCI metrics implementation, primarily:
 * <ul>
 *     <li>preparing connectivity to the backend ({@code Monitoring} and {@code TelemetryReporterBuilder}) and the
 *     OCI metrics {@code Metrics} object,</li>
 *     <li>maintaining the list of registries (typically just one), and</li>
 *     <li>managing the executor that periodically samples gauges and functional counters.</li>
 * </ul>
 * We sample gauges and functional counters periodically. Although application code or Helidon code updates counters,
 * timers, and distribution summaries in-line, gauges and functional counters mirror values that are updated outside the
 * metrics subsystem. Therefore, we cannot know when those values change without sampling them.
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
    private volatile ScheduledExecutorService gaugeSamplingExecutor;
    private volatile Monitoring monitoring;
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
                   "Registered Helidon OCI meter registry; registries={0}, gauges={1}",
                   registries.size(),
                   registry.gauges().size());
        start();
    }

    void start() {
        lifecycleLock.lock();
        try {
            if (closed.get() || started.get()) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Skipping Helidon OCI telemetry runtime start; closed={0}, started={1}",
                           closed.get(),
                           started.get());
                return;
            }
            LOGGER.log(System.Logger.Level.TRACE, "Starting Helidon OCI telemetry runtime");
            initializeRuntime();
            Main.addShutdownHandler(this);
            started.set(true);
            LOGGER.log(System.Logger.Level.TRACE, "Helidon OCI telemetry runtime started");
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
                LOGGER.log(System.Logger.Level.TRACE, "Skipping Helidon OCI telemetry runtime close; already closed");
                return;
            }
            LOGGER.log(System.Logger.Level.TRACE, "Closing Helidon OCI telemetry runtime");
            ScheduledExecutorService executor = gaugeSamplingExecutor;
            gaugeSamplingExecutor = null;
            if (executor != null) {
                LOGGER.log(System.Logger.Level.TRACE, "Stopping Helidon OCI gauge sampling executor");
                executor.shutdownNow();
            }
            publisher.stop();
            if (initializedMetrics && Metrics.isActive()) {
                LOGGER.log(System.Logger.Level.TRACE, "Shutting down OCI Metrics");
                Metrics.shutdown();
            }
            try {
                monitoring.close();
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Error closing Monitoring client", e);
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    /*
    Package-private so integration test can trigger gauge sampling deterministically.
     */
    void sampleGauges() {
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE,
                       "Sampling known gauges and functional counters across registries={0}",
                       registries.size());
        }
        registries.forEach(registry -> {
            registry.gauges().forEach(this::sampleGauge);
            registry.functionalCounters().forEach(this::sampleFunctionalCounter);
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
        if (config.project().isEmpty() || config.fleet().isEmpty()) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "OCI metrics provider is enabled but project/fleet are not fully configured; publishing is disabled.");
            publisher.stop();
            return;
        }

        if (Metrics.isActive()) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Helidon OCI metrics runtime found OCI Metrics object already initialized; Helidon OCI metrics config "
                               + "was not applied.");
        } else {
            monitoring = config.monitoring().orElseGet(() -> Services.get(Monitoring.class));
            Region region = RegionSupport.resolve(config.region(), () -> Services.get(Region.class));
            String publicRegionName = region.getPublicRegionName();
            LOGGER.log(System.Logger.Level.TRACE, "Resolved OCI region: {0}", region.getInternalName());
            LOGGER.log(System.Logger.Level.TRACE,
                       "Initializing OCI telemetry runtime; project={0}, fleet={1}, region={2}, monitoringFromConfig={3}",
                       config.project().orElse(""),
                       config.fleet().orElse(""),
                       publicRegionName,
                       config.monitoring().isPresent());
            TelemetryReporterBuilder builder = new TelemetryReporterBuilder()
                    .monitoringClient(monitoring)
                    .project(config.project().orElseThrow())
                    .fleet(config.fleet().orElseThrow())
                    .postMetricsRequestHeaders(config.requestHeaders());
            config.useMetadataService().ifPresent(builder::useMetadataService);
            config.overrideMetricKeys().ifPresent(builder::shouldOverrideMetricKeys);
            config.hostname().ifPresent(builder::hostname);
            config.availabilityDomain().ifPresent(builder::availabilityDomain);
            config.faultDomain().ifPresent(builder::faultDomain);
            builder.region(publicRegionName);
            TelemetryReporter reporter = builder.build();
            Metrics.init(reporter, config.defaultDimensions());
            initializedMetrics = true;
            LOGGER.log(System.Logger.Level.TRACE,
                       "Initialized telemetry Metrics runtime; defaultDimensions={0}",
                       config.defaultDimensions());
        }
        scheduleGaugeSampling();
    }

    private void scheduleGaugeSampling() {
        if (!config.sampleGauges()) {
            LOGGER.log(System.Logger.Level.TRACE, "Gauge sampling disabled");
            return;
        }
        var executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
                                                                          .name("Helidon OCI metrics gauge sampler")
                                                                          .factory());

        gaugeSamplingExecutor = executor;
        long intervalMillis = Math.max(1000L, config.gaugeSampleInterval().toMillis());
        LOGGER.log(System.Logger.Level.TRACE,
                   "Scheduling gauge sampling; intervalMillis={0}, registries={1}",
                   intervalMillis,
                   registries.size());
        executor.scheduleAtFixedRate(this::sampleGauges,
                                     intervalMillis,
                                     intervalMillis,
                                     TimeUnit.MILLISECONDS);
    }

    private void sampleGauge(OciGauge<?> gauge) {
        try {
            gauge.valueIfChanged().ifPresent(value -> publisher.publishGauge(gauge, value.doubleValue()));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Error sampling OCI-backed gauge " + gauge.id().name(), e);
        }
    }

    private void sampleFunctionalCounter(OciFunctionalCounter<?> functionalCounter) {
        try {
            functionalCounter.valueIfChanged()
                    .ifPresent(value -> publisher.publishFunctionalCounter(functionalCounter, value));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Error sampling OCI-backed functional counter " + functionalCounter.id().name(),
                       e);
        }
    }
}
