/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

final class EndpointConfigSupport implements Prototype.BuilderDecorator<EndpointConfig.BuilderBase<?, ?>> {
    private static final String CONNECT_TIMEOUT = "connect-timeout";
    private static final String CONNECTION_TIMEOUT = "connection-timeout";

    EndpointConfigSupport() {
    }

    @Override
    public void decorate(EndpointConfig.BuilderBase<?, ?> builder) {
        builder.config().ifPresent(config -> applyConnectionTimeoutAlias(config, builder));
    }

    private static void applyConnectionTimeoutAlias(Config config, EndpointConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(CONNECT_TIMEOUT).exists();
        boolean aliasExists = config.get(CONNECTION_TIMEOUT).exists();
        if (canonicalExists && aliasExists) {
            throw new IllegalArgumentException("Do not configure both " + CONNECT_TIMEOUT
                                                       + " and " + CONNECTION_TIMEOUT + "; specify only one.");
        }
        if (aliasExists) {
            builder.connectionTimeout().ifPresent(builder::connectTimeout);
        }
    }
}
