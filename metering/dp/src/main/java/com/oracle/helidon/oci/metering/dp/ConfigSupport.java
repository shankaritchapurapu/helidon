/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.time.Duration;

import io.helidon.builder.api.Prototype;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.service.registry.Services;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.bling.clients.BlingPublisherClient;

final class ConfigSupport {
    private ConfigSupport() {
    }

    static final class MeteringConfigSupport implements Prototype.BuilderDecorator<MeteringConfig.BuilderBase<?, ?>> {
        MeteringConfigSupport() {
        }

        @Override
        public void decorate(MeteringConfig.BuilderBase<?, ?> builder) {
            builder.reportInterval().ifPresent(MeteringConfigSupport::validateReportInterval);
            if (builder.blingPublisherClient().isEmpty()) {
                throw new ConfigException("MeteringConfig requires blingPublisherClient");
            }
        }

        private static void validateReportInterval(Duration duration) {
            if (duration.compareTo(Duration.ofSeconds(1)) < 0) {
                throw new ConfigException("MeteringConfig report-interval must be at least PT1S");
            }
        }

        @Prototype.ConfigFactoryMethod("blingPublisherClient")
        static BlingPublisherClient createBlingPublisherClient(Config config) {
            return BlingPublisherClientConfig.create(config).build();
        }

    }

    static final class BlingPublisherClientSupport {
        private BlingPublisherClientSupport() {
        }

        @Prototype.RuntimeTypeFactoryMethod
        static BlingPublisherClient createBlingPublisherClient(BlingPublisherClientConfig config) {
            return new BlingPublisherClient(config.endpoint(), config.clientId(), authProvider());
        }
    }

    private static BasicAuthenticationDetailsProvider authProvider() {
        return Services.get(BasicAuthenticationDetailsProvider.class);
    }
}
