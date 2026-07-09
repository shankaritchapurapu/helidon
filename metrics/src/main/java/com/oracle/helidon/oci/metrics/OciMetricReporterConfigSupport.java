/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.builder.api.Prototype;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;

final class OciMetricReporterConfigSupport
        implements Prototype.BuilderDecorator<OciMetricReporterConfigBase.BuilderBase<?, ?>> {
    private static final String OCI_ENV_AVAILABILITY_DOMAIN = "oci.env.availability-domain";
    private static final String OCI_ENV_FAULT_DOMAIN = "oci.env.fault-domain";

    OciMetricReporterConfigSupport() {
    }

    @Override
    public void decorate(OciMetricReporterConfigBase.BuilderBase<?, ?> builder) {
        applyHostNameAlias(builder);
        builder.config()
                .map(Config::config)
                .ifPresent(config -> applyLocationDefaults(config, builder));
    }

    private static void applyHostNameAlias(OciMetricReporterConfigBase.BuilderBase<?, ?> builder) {
        if (builder.hostname().isPresent() && builder.hostName().isPresent()) {
            throw new ConfigException("Do not configure both hostname and host-name; specify only one.");
        }
        if (builder.hostname().isEmpty()) {
            builder.hostName().ifPresent(builder::hostname);
        }
    }

    private static void applyLocationDefaults(Config config, OciMetricReporterConfigBase.BuilderBase<?, ?> builder) {
        Config rootConfig = config.root();
        if (builder.availabilityDomain().isEmpty()) {
            rootConfig.get(OCI_ENV_AVAILABILITY_DOMAIN)
                    .asString()
                    .ifPresent(builder::availabilityDomain);
        }
        if (builder.faultDomain().isEmpty()) {
            rootConfig.get(OCI_ENV_FAULT_DOMAIN)
                    .asString()
                    .ifPresent(builder::faultDomain);
        }
    }
}
