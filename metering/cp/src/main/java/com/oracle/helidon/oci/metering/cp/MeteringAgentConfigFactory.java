/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Duration;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

/**
 * Factory that adapts Helidon control plane metering configuration to the native emitter configuration.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class MeteringAgentConfigFactory implements Supplier<com.oracle.pic.bling.emit.config.MeteringAgentConfig> {
    private final MeteringConfig meteringConfig;

    @Service.Inject
    MeteringAgentConfigFactory(MeteringConfig meteringConfig) {
        this.meteringConfig = meteringConfig;
    }

    @Override
    public com.oracle.pic.bling.emit.config.MeteringAgentConfig get() {
        var bucketConfigs = meteringConfig.bucketConfigs()
                .stream()
                .map(MeteringAgentConfigFactory::nativeBucketConfig)
                .toList();

        var builder = com.oracle.pic.bling.emit.config.MeteringAgentConfig.builder()
                .endpoint(meteringConfig.endpoint())
                .clientId(meteringConfig.clientId())
                .bucketConfigs(bucketConfigs);

        meteringConfig.maxWorkers().ifPresent(builder::maxWorkers);
        meteringConfig.meteringPeriod().map(MeteringAgentConfigFactory::seconds).ifPresent(builder::meteringPeriodInSeconds);
        meteringConfig.canaryDisabled().ifPresent(builder::canaryDisabled);
        meteringConfig.maxArchiveWorkers().ifPresent(builder::maxArchiveWorkers);
        meteringConfig.scanPageSize().ifPresent(builder::scanPageSize);
        meteringConfig.maxWritesPerTransaction().ifPresent(builder::maxWritesPerTransaction);
        meteringConfig.retentionPeriod().map(MeteringAgentConfigFactory::days).ifPresent(builder::retentionPeriodDays);
        meteringConfig.skipArchiveLeaseCheck().ifPresent(builder::skipArchiveLeaseCheck);
        meteringConfig.leaseDaoScanPageSize().ifPresent(builder::leaseDAOScanPageSize);
        meteringConfig.fastCatchupModeEnabled().ifPresent(builder::fastCatchupModeEnabled);

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
