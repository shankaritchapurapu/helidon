/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.helidon.builder.api.RuntimeType;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.spi.ServerFeature;

/**
 * Server features are automatically discovered by Helidon WebServer when running with
 * {@link io.helidon.service.registry.ServiceRegistryManager#start(io.helidon.service.registry.Binding)}.
 */
@RuntimeType.PrototypedBy(SplatMtlsConfig.class)
public class SplatMtlsFeature implements ServerFeature, RuntimeType.Api<SplatMtlsConfig> {
    private static final Logger LOGGER = Logger.getLogger(SplatMtlsFilter.class.getName());
    private final SplatMtlsConfig config;
    static final String SPLAT_ID = "splat";

    SplatMtlsFeature(SplatMtlsConfig config) {
        this.config = config;
    }

    /**
     * Create a new plat mTLS feature based on its configuration.
     *
     * @param config configuration
     * @return a new observe feature
     */
    public static SplatMtlsFeature create(SplatMtlsConfig config) {
        return new SplatMtlsFeature(config);
    }

    /**
     * Create a new Splat mTLS feature customizing its configuration.
     *
     * @param consumer configuration consumer
     * @return a new observe feature
     */
    public static SplatMtlsFeature create(Consumer<SplatMtlsConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    /**
     * A new builder to customize Splat mTLS feature support.
     *
     * @return a new builder
     */
    public static SplatMtlsConfig.Builder builder() {
        return SplatMtlsConfig.builder();
    }

    @Override
    public void setup(ServerFeatureContext serverFeatureContext) {
        if (config.enabled()) {
            List<String> sockets = config.sockets();
            SplatMtlsFilter splatMtlsFilter = new SplatMtlsFilter(config);
            if (sockets.isEmpty()) {
                serverFeatureContext.socket(WebServer.DEFAULT_SOCKET_NAME)
                        .httpRouting()
                        .addFilter(splatMtlsFilter);
            } else {
                sockets.stream()
                        .map(it -> serverFeatureContext.socket(it)
                                .httpRouting()
                                .addFilter(splatMtlsFilter)
                        );
            }
        } else {
            LOGGER.info("SplatMtlsFeature is disabled");
        }
    }

    @Override
    public String name() {
        return SPLAT_ID;
    }

    @Override
    public String type() {
        return SPLAT_ID;
    }

    @Override
    public SplatMtlsConfig prototype() {
        return config;
    }
}
