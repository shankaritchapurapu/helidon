/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.requestid.webserver;

import io.helidon.common.config.Config;
import io.helidon.webserver.spi.ServerFeatureProvider;

/**
 * {@link java.util.ServiceLoader} provider implementation to add OCI request ID feature to
 * Helidon WebServer.
 */
public class RequestIdServerFeatureProvider implements ServerFeatureProvider<RequestIdServerFeature> {
    /**
     * Required by {@link java.util.ServiceLoader}.
     */
    public RequestIdServerFeatureProvider() {
    }

    @Override
    public String configKey() {
        return RequestIdServerFeature.TYPE;
    }

    @Override
    public RequestIdServerFeature create(Config config, String name) {
        return RequestIdServerFeature.create(config, name);
    }
}
