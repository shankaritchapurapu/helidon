/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

final class Ssv2ClientConfigSupport implements Prototype.BuilderDecorator<Ssv2ClientConfig.BuilderBase<?, ?>> {
    private static final String RETRY_CONFIG = "retry-config";
    private static final String RETRY = "retry";

    Ssv2ClientConfigSupport() {
    }

    @Override
    public void decorate(Ssv2ClientConfig.BuilderBase<?, ?> builder) {
        builder.config().ifPresent(config -> applyRetryAlias(config, builder));
    }

    private static void applyRetryAlias(Config config, Ssv2ClientConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(RETRY_CONFIG).exists();
        boolean aliasExists = config.get(RETRY).exists();
        if (canonicalExists && aliasExists) {
            throw new IllegalArgumentException("Do not configure both " + RETRY_CONFIG
                                                       + " and " + RETRY + "; specify only one.");
        }
        if (aliasExists) {
            builder.retry().ifPresent(builder::retryConfig);
        }
    }
}
