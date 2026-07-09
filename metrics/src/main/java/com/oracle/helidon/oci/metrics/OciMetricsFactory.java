/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.metrics.api.Clock;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionStatisticsConfig;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.HistogramSnapshot;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.MetricsPublisher;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.api.Timer;
import io.helidon.metrics.spi.MetersProvider;
import io.helidon.service.registry.Services;

/**
 * Metrics factory for the OCI metrics provider.
 */
@Weight(Weighted.DEFAULT_WEIGHT + 50.0)
final class OciMetricsFactory implements MetricsFactory {

    private static final System.Logger LOGGER = System.getLogger(OciMetricsFactory.class.getName());

    private static final Clock SYSTEM_CLOCK = new Clock() {
        @Override
        public long wallTime() {
            return System.currentTimeMillis();
        }

        @Override
        public long monotonicTime() {
            return System.nanoTime();
        }

        @Override
        public <R> R unwrap(Class<? extends R> c) {
            return c.cast(this);
        }
    };

    private final MetricsFactory delegate;
    private final OciMetricsPublisherConfig publisherConfig;
    private final MetricsConfig metricsConfig;
    private final MetricsConfig delegateMetricsConfig;
    private final List<MeterRegistry> registries = new CopyOnWriteArrayList<>();
    private final OciTelemetryRuntime runtime;
    private final Clock systemClock = SYSTEM_CLOCK;
    private final List<MetersProvider> metersProviders;
    private volatile MeterRegistry globalRegistry;

    OciMetricsFactory(MetricsFactory delegate,
                      OciMetricsPublisherConfig publisherConfig,
                      MetricsConfig metricsConfig,
                      java.util.Collection<MetersProvider> metersProviders) {
        this.delegate = delegate;
        this.publisherConfig = publisherConfig;
        this.metricsConfig = metricsConfig;
        this.delegateMetricsConfig = delegateMetricsConfig(metricsConfig);
        this.runtime = new OciTelemetryRuntime(publisherConfig);
        this.metersProviders = List.copyOf(metersProviders);
    }

    @Override
    public void close() {
        runtime.close();
        registries.forEach(MeterRegistry::close);
        delegate.close();
    }

    @Override
    public MeterRegistry globalRegistry() {
        return globalRegistry(metricsConfig);
    }

    @Override
    public synchronized MeterRegistry globalRegistry(MetricsConfig metricsConfig) {
        if (globalRegistry != null) {
            if (metricsConfig.equals(this.metricsConfig)) {
                return globalRegistry;
            }
            /*
            Ideally this method is invoked exactly once, but on subsequent invocations with different configuration
            clear the previous one and create a new one with the specified settings.
             */
            globalRegistry.close();
        }
        globalRegistry = createMeterRegistry(metricsConfig);
        return globalRegistry;
    }

    @Override
    public MeterRegistry globalRegistry(Consumer<Meter> onAdd, Consumer<Meter> onRemove, boolean backfill) {
        var result = globalRegistry == null ? globalRegistry(metricsConfig) : globalRegistry;
        result.onMeterAdded(onAdd);
        result.onMeterRemoved(onRemove);

        if (backfill) {
            result.meters().forEach(onAdd);
        }
        return result;
    }

    @Override
    public MetricsConfig metricsConfig() {
        return metricsConfig;
    }

    @Override
    public <B extends MeterRegistry.Builder<B, M>, M extends MeterRegistry> B meterRegistryBuilder() {
        throw new UnsupportedOperationException("Custom OCI meter registry builders are not implemented");
    }

    @Override
    public MeterRegistry createMeterRegistry(MetricsConfig metricsConfig) {
        return createMeterRegistry(systemClock, metricsConfig, meter -> {
        }, meter -> {
        });
    }

    @Override
    public MeterRegistry createMeterRegistry(MetricsConfig metricsConfig,
                                             Consumer<Meter> onAdd,
                                             Consumer<Meter> onRemove) {
        return createMeterRegistry(systemClock, metricsConfig, onAdd, onRemove);
    }

    @Override
    public MeterRegistry createMeterRegistry(Clock clock, MetricsConfig metricsConfig) {
        return createMeterRegistry(clock, metricsConfig, meter -> {
        }, meter -> {
        });
    }

    @Override
    public MeterRegistry createMeterRegistry(Clock clock,
                                             MetricsConfig metricsConfig,
                                             Consumer<Meter> onAdd,
                                             Consumer<Meter> onRemove) {
        LOGGER.log(System.Logger.Level.TRACE,
                   "Creating OCI meter registry; publisherEnabled={0}, metersProviders={1}",
                   publisherConfig.enabled(),
                   metersProviders.size());
        MeterRegistry delegateRegistry = delegate.createMeterRegistry(clock, delegateMetricsConfig);
        OciMeterRegistry registry = new OciMeterRegistry(metricsConfig,
                                                         clock,
                                                         onAdd,
                                                         onRemove,
                                                         runtime.publisher(),
                                                         delegate,
                                                         delegateRegistry);

        Services.add(OciMeterRegistry.class, Weighted.DEFAULT_WEIGHT, registry);

        registries.add(registry);
        runtime.registerRegistry(registry);
        metersProviders.forEach(provider -> provider.meterBuilders(this).forEach(builder -> registerMeter(registry, builder)));
        LOGGER.log(System.Logger.Level.TRACE,
                   "Created OCI meter registry; currentMeterCount={0}, currentGaugeCount={1}",
                   registry.meters().size(),
                   registry.gauges().size());
        return registry;
    }

    @Override
    public Clock clockSystem() {
        return systemClock;
    }

    @Override
    public Counter.Builder counterBuilder(String name) {
        return OciCounter.builder(name);
    }

    @Override
    public <T> io.helidon.metrics.api.FunctionalCounter.Builder<T> functionalCounterBuilder(String name,
                                                                                            T instance,
                                                                                            Function<T, Long> countFunction) {
        return OciFunctionalCounter.builder(name, instance, countFunction);
    }

    @Override
    public DistributionStatisticsConfig.Builder distributionStatisticsConfigBuilder() {
        return OciDistributionStatisticsConfig.builder();
    }

    @Override
    public DistributionSummary.Builder distributionSummaryBuilder(String name,
                                                                  DistributionStatisticsConfig.Builder configBuilder) {
        return OciDistributionSummary.builder(name, configBuilder);
    }

    @Override
    public <T> Gauge.Builder<Double> gaugeBuilder(String name, T instance, java.util.function.ToDoubleFunction<T> valueFunction) {
        return OciGauge.builder(name, instance, valueFunction);
    }

    @Override
    public <N extends Number> Gauge.Builder<N> gaugeBuilder(String name, Supplier<N> supplier) {
        return OciGauge.builder(name, supplier);
    }

    @Override
    public Timer.Builder timerBuilder(String name) {
        return OciTimer.builder(name);
    }

    @Override
    public Timer.Sample timerStart() {
        return OciTimer.start();
    }

    @Override
    public Timer.Sample timerStart(MeterRegistry meterRegistry) {
        return OciTimer.start(meterRegistry);
    }

    @Override
    public Timer.Sample timerStart(Clock clock) {
        return OciTimer.start(clock);
    }

    @Override
    public Tag tagCreate(String key, String value) {
        return new OciTag(key, value);
    }

    @Override
    public HistogramSnapshot histogramSnapshotEmpty(long count, double total, double max) {
        return HistogramSnapshot.empty(count, total, max);
    }

    OciMetricsPublisherConfig publisherConfig() {
        return publisherConfig;
    }

    OciTelemetryRuntime runtime() {
        return runtime;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerMeter(OciMeterRegistry registry, Meter.Builder<?, ?> builder) {
        registry.getOrCreate((Meter.Builder) builder);
    }

    private static MetricsConfig delegateMetricsConfig(MetricsConfig metricsConfig) {
        /*
        The underlying delegate should not see the OCI publishers which would suppress any default publisher it would normally
        provide.
         */
        List<MetricsPublisher> delegatePublishers = metricsConfig.publishers()
                .stream()
                .filter(publisher -> !(publisher instanceof OciMetricsPublisher))
                .filter(publisher -> !OciMetricsPublisher.TYPE.equals(publisher.type()))
                .toList();
        MetricsConfig.Builder builder = MetricsConfig.builder(metricsConfig)
                .clearPublishers()
                .addPublishers(delegatePublishers)
                .publishersDiscoverServices(false);
        if (delegatePublishers.isEmpty()) {
            builder.config(io.helidon.config.Config.empty());
        }
        return builder.build();
    }
}
