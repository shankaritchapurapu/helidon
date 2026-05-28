/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

final class AuthorizationConfigSupport implements Prototype.BuilderDecorator<AuthorizationConfig.BuilderBase<?, ?>> {
    private static final String SERVICE_NAME = "service-name";
    private static final String SERVICE = "service";

    AuthorizationConfigSupport() {
    }

    @Override
    public void decorate(AuthorizationConfig.BuilderBase<?, ?> builder) {
        builder.config().ifPresent(config -> applyServiceAlias(config, builder));
    }

    private static void applyServiceAlias(Config config, AuthorizationConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(SERVICE_NAME).exists();
        boolean aliasExists = config.get(SERVICE).exists();
        if (canonicalExists && aliasExists) {
            throw new IllegalArgumentException("Do not configure both " + SERVICE_NAME
                                                       + " and " + SERVICE + "; specify only one.");
        }
        if (aliasExists) {
            builder.service().ifPresent(builder::serviceName);
        }
    }
}
