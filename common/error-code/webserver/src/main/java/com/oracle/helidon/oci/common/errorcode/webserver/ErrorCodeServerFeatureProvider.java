/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode.webserver;

import io.helidon.common.config.Config;
import io.helidon.webserver.spi.ServerFeatureProvider;

/**
 * {@link java.util.ServiceLoader} provider implementation to add error handling to Helidon
 * WebServer.
 * There is no conflict when adding webserver error handler and JAX-RS error handler, as JAX-RS
 * never re-throws exceptions outside of its scope.
 */
public class ErrorCodeServerFeatureProvider implements ServerFeatureProvider<ErrorCodeServerFeature> {
    /**
     * Create a new instance, intended for {@link java.util.ServiceLoader} only.
     */
    public ErrorCodeServerFeatureProvider() {
    }

    @Override
    public String configKey() {
        return ErrorCodeServerFeature.TYPE;
    }

    @Override
    public ErrorCodeServerFeature create(Config config, String name) {
        return ErrorCodeServerFeature.create(config, name);
    }
}
