/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

final class OciMetricsPublisherConfigSupport
        implements Prototype.BuilderDecorator<OciMetricsPublisherConfig.BuilderBase<?, ?>> {
    private static final String HOSTNAME = "hostname";
    private static final String HOST_NAME = "host-name";

    OciMetricsPublisherConfigSupport() {
    }

    @Override
    public void decorate(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        builder.config().ifPresent(config -> applyHostNameAlias(config, builder));
    }

    private static void applyHostNameAlias(Config config, OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(HOSTNAME).exists();
        boolean aliasExists = config.get(HOST_NAME).exists();
        if (canonicalExists && aliasExists) {
            throw new IllegalArgumentException("Do not configure both " + HOSTNAME
                                                       + " and " + HOST_NAME + "; specify only one.");
        }
        if (aliasExists) {
            builder.hostName().ifPresent(builder::hostname);
        }
    }
}
