/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Duration;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory that adapts Helidon control plane metering configuration to the native emitter configuration.
 */
@Service.Singleton
class MeteringAgentConfigFactory implements Supplier<com.oracle.pic.bling.emit.config.MeteringAgentConfig> {
    private final Config config;

    @Service.Inject
    MeteringAgentConfigFactory(Config config) {
        this.config = config;
    }

    @Override
    public com.oracle.pic.bling.emit.config.MeteringAgentConfig get() {
        AgentMeteringConfig agentConfig = AgentMeteringConfig.create(config.get("oci.metering.agent"));
        var bucketConfigs = agentConfig.bucketConfigs()
                .stream()
                .map(MeteringAgentConfigFactory::nativeBucketConfig)
                .toList();

        var builder = com.oracle.pic.bling.emit.config.MeteringAgentConfig.builder()
                .endpoint(agentConfig.endpoint())
                .clientId(agentConfig.clientId())
                .bucketConfigs(bucketConfigs);

        agentConfig.maxWorkers().ifPresent(builder::maxWorkers);
        agentConfig.meteringPeriod().map(MeteringAgentConfigFactory::seconds).ifPresent(builder::meteringPeriodInSeconds);
        agentConfig.canaryDisabled().ifPresent(builder::canaryDisabled);
        agentConfig.maxArchiveWorkers().ifPresent(builder::maxArchiveWorkers);
        agentConfig.scanPageSize().ifPresent(builder::scanPageSize);
        agentConfig.maxWritesPerTransaction().ifPresent(builder::maxWritesPerTransaction);
        agentConfig.retentionPeriod().map(MeteringAgentConfigFactory::days).ifPresent(builder::retentionPeriodDays);
        agentConfig.skipArchiveLeaseCheck().ifPresent(builder::skipArchiveLeaseCheck);
        agentConfig.leaseDaoScanPageSize().ifPresent(builder::leaseDAOScanPageSize);
        agentConfig.fastCatchupModeEnabled().ifPresent(builder::fastCatchupModeEnabled);

        com.oracle.pic.bling.emit.config.MeteringAgentConfig nativeConfig = builder.build();
        nativeConfig.validate();
        return nativeConfig;
    }

    private static com.oracle.pic.bling.emit.config.MeteringBucketConfig nativeBucketConfig(MeteringBucketConfig config) {
        return com.oracle.pic.bling.emit.config.MeteringBucketConfig.builder()
                .bucketName(config.bucketName())
                .serviceName(config.serviceName())
                .meterName(config.meterName())
                .build();
    }

    private static int seconds(Duration duration) {
        return Math.toIntExact(duration.toSeconds());
    }

    private static int days(Duration duration) {
        return Math.toIntExact(duration.toDays());
    }
}
