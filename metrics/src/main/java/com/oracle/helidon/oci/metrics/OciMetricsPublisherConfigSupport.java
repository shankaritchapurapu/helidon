/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.function.BiFunction;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.metrics.api.Meter;

final class OciMetricsPublisherConfigSupport
        implements Prototype.BuilderDecorator<OciMetricsPublisherConfig.BuilderBase<?, ?>> {
    static final String DEFAULT_METRICS_SCOPE_NAME = "service";

    private static final String HOSTNAME = "hostname";
    private static final String HOST_NAME = "host-name";
    private static final String OCI_ENV_AVAILABILITY_DOMAIN = "oci.env.availability-domain";
    private static final String OCI_ENV_FAULT_DOMAIN = "oci.env.fault-domain";

    OciMetricsPublisherConfigSupport() {
    }

    @Override
    public void decorate(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        builder.config().ifPresent(config -> {
            applyHostNameAlias(config, builder);
            applyLocationDefaults(config, builder);
        });
        validateMetricFilterModes(builder);
        applyMetricFilter(builder);
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

    private static void applyLocationDefaults(Config config, OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        if (builder.availabilityDomain().isPresent() && builder.faultDomain().isPresent()) {
            return;
        }
        /*
         * Publisher config is nested under metrics.publishers[], while oci-env publishes
         * location defaults at oci.env. Use the nested config root only when a location
         * field is missing, and never replace explicit publisher values.
         */
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

    private static void applyMetricFilter(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        BiFunction<String, Meter, Boolean> filter = builder.filter().orElse(null);
        // Refresh a copied or previously generated default filter so it reflects the builder's current settings.
        if (filter == null || filter instanceof ReporterMetricFilter) {
            builder.filter(ReporterMetricFilter.create(builder));
        }
    }

    private static void validateMetricFilterModes(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        if (builder.useRegexFilters() && builder.useSubstringMatching()) {
            throw new ConfigException("OCI metrics publisher filter configuration is ambiguous: "
                                              + "do not enable both use-regex-filters and use-substring-matching; "
                                              + "choose either regex or substring matching.");
        }
    }
}
