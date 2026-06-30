/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;
import io.helidon.metrics.spi.MetersProvider;
import io.helidon.metrics.spi.MetricsFactoryProvider;
import io.helidon.service.registry.Service;

/**
 * Provider for the metrics factory implementation for Helidon Talon metrics.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT + 100.0)
public class OciMetricsFactoryProvider implements MetricsFactoryProvider {

    private final MetricsFactoryProvider delegate;
    private final List<OciMetricsFactory> factories = new ArrayList<>();

    /**
     * Creates a new provider.
     */
    public OciMetricsFactoryProvider() {
        this(new MicrometerMetricsFactoryProvider());
    }

    /**
     * Creates a new provider.
     *
     * @param delegate underlying metrics factory provider
     */
    @Service.Inject
    public OciMetricsFactoryProvider(MetricsFactoryProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public MetricsFactory create(Config config,
                                 MetricsConfig metricsConfig,
                                 Collection<MetersProvider> metersProviders) {
        var delegateMetricsFactory = delegate.create(config, metricsConfig, metersProviders);

        OciMetricsPublisherConfig publisherConfig = metricsConfig.publishers()
                .stream()
                .filter(OciMetricsPublisher.class::isInstance)
                .map(OciMetricsPublisher.class::cast)
                .map(OciMetricsPublisher::prototype)
                .findFirst()
                .orElseGet(() -> OciMetricsPublisherConfig.builder()
                        .enabled(false)
                        .defaultDimensions(Map.of())
                        .requestHeaders(Map.of())
                        .buildPrototype());
        OciMetricsFactory factory = new OciMetricsFactory(delegateMetricsFactory,
                                                          publisherConfig,
                                                          metricsConfig,
                                                          metersProviders);
        factories.add(factory);
        return factory;
    }

    @Override
    public void close() {
        factories.forEach(OciMetricsFactory::close);
        factories.clear();
    }
}
