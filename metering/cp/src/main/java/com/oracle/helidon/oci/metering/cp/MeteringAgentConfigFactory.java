/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Duration;
import java.util.Set;
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
        var bucketV3Configs = meteringConfig.bucketV3Configs()
                .stream()
                .map(MeteringAgentConfigFactory::nativeBucketV3Config)
                .toList();

        var builder = com.oracle.pic.bling.emit.config.MeteringAgentConfig.builder()
                .endpoint(meteringConfig.endpoint())
                .clientId(meteringConfig.clientId())
                .bucketConfigs(bucketConfigs)
                .bucketV3Configs(bucketV3Configs);

        meteringConfig.maxWorkers().ifPresent(builder::maxWorkers);
        meteringConfig.meteringPeriod().map(MeteringAgentConfigFactory::seconds).ifPresent(builder::meteringPeriodInSeconds);
        meteringConfig.leaseDuration().map(MeteringAgentConfigFactory::seconds).ifPresent(builder::leaseDurationInSeconds);
        meteringConfig.canaryDisabled().ifPresent(builder::canaryDisabled);
        meteringConfig.maxArchiveWorkers().ifPresent(builder::maxArchiveWorkers);
        meteringConfig.scanPageSize().ifPresent(builder::scanPageSize);
        meteringConfig.maxWritesPerTransaction().ifPresent(builder::maxWritesPerTransaction);
        meteringConfig.retentionPeriod().map(MeteringAgentConfigFactory::days).ifPresent(builder::retentionPeriodDays);
        meteringConfig.skipArchiveLeaseCheck().ifPresent(builder::skipArchiveLeaseCheck);
        meteringConfig.leaseDaoScanPageSize().ifPresent(builder::leaseDAOScanPageSize);
        meteringConfig.fastCatchupModeEnabled().ifPresent(builder::fastCatchupModeEnabled);
        meteringConfig.duplicateV2WritesDisabled().ifPresent(builder::duplicateV2WritesDisabled);
        meteringConfig.archiverTimeout().map(MeteringAgentConfigFactory::minutes).ifPresent(builder::archiverTimeOut);

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

    private static com.oracle.pic.bling.emit.config.MeteringBucketConfigV3 nativeBucketV3Config(MeteringBucketConfigV3 config) {
        return com.oracle.pic.bling.emit.config.MeteringBucketConfigV3.builder()
                .bucketName(config.bucketName())
                .serviceName(config.serviceName())
                .meterNames(Set.copyOf(config.meterNames()))
                .build();
    }

    private static int seconds(Duration duration) {
        if (duration.getNano() != 0) {
            throw new IllegalArgumentException("Metering duration must be a whole number of seconds: " + duration);
        }
        return Math.toIntExact(duration.toSeconds());
    }

    private static int minutes(Duration duration) {
        if (duration.getNano() != 0 || duration.getSeconds() % 60 != 0) {
            throw new IllegalArgumentException("Archiver timeout must be a whole number of minutes: " + duration);
        }
        return Math.toIntExact(duration.toMinutes());
    }

    private static int days(Duration duration) {
        if (duration.getNano() != 0 || duration.getSeconds() % Duration.ofDays(1).toSeconds() != 0) {
            throw new IllegalArgumentException("Retention period must be a whole number of days: " + duration);
        }
        return Math.toIntExact(duration.toDays());
    }
}
