/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.time.Duration;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

/**
 * Factory that adapts Helidon data plane metering configuration to the native emitter configuration.
 */
@Service.Singleton
class MeteringAgentConfigFactory implements Supplier<com.oracle.pic.bling.config.MeteringAgentConfig> {
    private final MeteringConfig config;

    @Service.Inject
    MeteringAgentConfigFactory(MeteringConfig config) {
        this.config = config;
    }

    @Override
    public com.oracle.pic.bling.config.MeteringAgentConfig get() {
        var builder = com.oracle.pic.bling.config.MeteringAgentConfig.builder()
                .endpoint(config.endpoint())
                .meteringDir(config.meteringDir())
                .clientId(config.clientId())
                .service(config.service());

        config.meteringPeriod().map(MeteringAgentConfigFactory::seconds).ifPresent(builder::meteringPeriodInSeconds);
        config.archivingDuration().map(MeteringAgentConfigFactory::seconds).ifPresent(builder::archivingDurationInSeconds);
        config.osEnabled().ifPresent(builder::osEnabled);
        config.k8sBasedDeployment().ifPresent(builder::isK8sBasedDeployment);
        config.bucketName().ifPresent(builder::bucketName);
        config.namespace().ifPresent(builder::namespace);
        config.reportInterval().map(MeteringAgentConfigFactory::seconds).ifPresent(builder::reportToBlingFrequency);

        com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig = builder.build();
        nativeConfig.validate();
        return nativeConfig;
    }

    private static int seconds(Duration duration) {
        return Math.toIntExact(duration.toSeconds());
    }
}
