/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.helidon.common.testing.junit5.OptionalMatcher;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class MeteringConfigFactoryTest {

    @Test
    void loadsConfiguredValues() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.ofEntries(
                Map.entry("oci.metering.endpoint", "https://bling-cp.example"),
                Map.entry("oci.metering.client-id", "cp-client"),
                Map.entry("oci.metering.region", "us-phoenix-1"),
                Map.entry("oci.metering.enabled", "false"),
                Map.entry("oci.metering.host-name", "cp-host"),
                Map.entry("oci.metering.max-workers", "4"),
                Map.entry("oci.metering.metering-period", "PT75S"),
                Map.entry("oci.metering.canary-disabled", "true"),
                Map.entry("oci.metering.bucket-configs.0.bucket-name", "first-bucket"),
                Map.entry("oci.metering.bucket-configs.0.service-name", "first-service"),
                Map.entry("oci.metering.bucket-configs.0.meter-name", "first-meter"),
                Map.entry("oci.metering.bucket-configs.1.bucket-name", "second-bucket"),
                Map.entry("oci.metering.bucket-configs.1.service-name", "second-service"),
                Map.entry("oci.metering.bucket-configs.1.meter-name", "second-meter"),
                Map.entry("oci.metering.max-archive-workers", "5"),
                Map.entry("oci.metering.scan-page-size", "250"),
                Map.entry("oci.metering.max-writes-per-transaction", "25"),
                Map.entry("oci.metering.retention-period", "PT72H"),
                Map.entry("oci.metering.skip-archive-lease-check", "true"),
                Map.entry("oci.metering.lease-dao-scan-page-size", "125"),
                Map.entry("oci.metering.fast-catchup-mode-enabled", "true")
        ))).get();

        assertThat(config.endpoint(), is("https://bling-cp.example"));
        assertThat(config.clientId(), is("cp-client"));
        assertThat(config.region(), OptionalMatcher.optionalValue(is("us-phoenix-1")));
        assertThat(config.enabled(), is(false));
        assertThat(config.hostName(), is(Optional.of("cp-host")));
        assertThat(config.maxWorkers(), is(Optional.of(4)));
        assertThat(config.meteringPeriod(), is(Optional.of(Duration.ofSeconds(75))));
        assertThat(config.canaryDisabled(), is(Optional.of(true)));
        assertThat(config.bucketConfigs().size(), is(2));
        assertBucketConfig(config.bucketConfigs().getFirst(), "first-bucket", "first-service", "first-meter");
        assertBucketConfig(config.bucketConfigs().get(1), "second-bucket", "second-service", "second-meter");
        assertThat(config.maxArchiveWorkers(), is(Optional.of(5)));
        assertThat(config.scanPageSize(), is(Optional.of(250)));
        assertThat(config.maxWritesPerTransaction(), is(Optional.of(25)));
        assertThat(config.retentionPeriod(), is(Optional.of(Duration.ofHours(72))));
        assertThat(config.skipArchiveLeaseCheck(), is(Optional.of(true)));
        assertThat(config.leaseDaoScanPageSize(), is(Optional.of(125)));
        assertThat(config.fastCatchupModeEnabled(), is(Optional.of(true)));
    }

    @Test
    void loadsOnlyRequiredValues() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.of(
                "oci.metering.endpoint", "https://bling-cp.example",
                "oci.metering.client-id", "cp-client",
                "oci.metering.region", "us-phoenix-1"
        ))).get();

        assertThat(config.endpoint(), is("https://bling-cp.example"));
        assertThat(config.clientId(), is("cp-client"));
        assertThat(config.region(), OptionalMatcher.optionalValue(is("us-phoenix-1")));
        assertThat(config.enabled(), is(true));
        assertThat(config.hostName(), is(Optional.empty()));
        assertThat(config.maxWorkers(), is(Optional.empty()));
        assertThat(config.meteringPeriod(), is(Optional.empty()));
        assertThat(config.canaryDisabled(), is(Optional.empty()));
        assertThat(config.bucketConfigs().isEmpty(), is(true));
        assertThat(config.maxArchiveWorkers(), is(Optional.empty()));
        assertThat(config.scanPageSize(), is(Optional.empty()));
        assertThat(config.maxWritesPerTransaction(), is(Optional.empty()));
        assertThat(config.retentionPeriod(), is(Optional.empty()));
        assertThat(config.skipArchiveLeaseCheck(), is(Optional.empty()));
        assertThat(config.leaseDaoScanPageSize(), is(Optional.empty()));
        assertThat(config.fastCatchupModeEnabled(), is(Optional.empty()));
    }

    @Test
    void convertsToNativeConfig() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.ofEntries(
                Map.entry("oci.metering.endpoint", "https://bling-cp.example"),
                Map.entry("oci.metering.client-id", "cp-client"),
                Map.entry("oci.metering.region", "us-phoenix-1"),
                Map.entry("oci.metering.max-workers", "6"),
                Map.entry("oci.metering.metering-period", "PT3M"),
                Map.entry("oci.metering.canary-disabled", "true"),
                Map.entry("oci.metering.bucket-configs.0.bucket-name", "archive-bucket"),
                Map.entry("oci.metering.bucket-configs.0.service-name", "archive-service"),
                Map.entry("oci.metering.bucket-configs.0.meter-name", "archive-meter"),
                Map.entry("oci.metering.max-archive-workers", "7"),
                Map.entry("oci.metering.scan-page-size", "300"),
                Map.entry("oci.metering.max-writes-per-transaction", "30"),
                Map.entry("oci.metering.retention-period", "PT96H"),
                Map.entry("oci.metering.skip-archive-lease-check", "true"),
                Map.entry("oci.metering.lease-dao-scan-page-size", "150"),
                Map.entry("oci.metering.fast-catchup-mode-enabled", "true")
        ))).get();

        com.oracle.pic.bling.emit.config.MeteringAgentConfig nativeConfig =
                new MeteringAgentConfigFactory(config).get();

        assertThat(nativeConfig.getEndpoint(), is("https://bling-cp.example"));
        assertThat(nativeConfig.getClientId(), is("cp-client"));
        assertThat(nativeConfig.getMaxWorkers(), is(6));
        assertThat(nativeConfig.getMeteringPeriodInSeconds(), is(180));
        assertThat(nativeConfig.isCanaryDisabled(), is(true));
        assertThat(nativeConfig.getBucketConfigs().size(), is(1));
        assertThat(nativeConfig.getBucketConfigs().getFirst().getBucketName(), is("archive-bucket"));
        assertThat(nativeConfig.getBucketConfigs().getFirst().getServiceName(), is("archive-service"));
        assertThat(nativeConfig.getBucketConfigs().getFirst().getMeterName(), is("archive-meter"));
        assertThat(nativeConfig.getMaxArchiveWorkers(), is(7));
        assertThat(nativeConfig.getScanPageSize(), is(300));
        assertThat(nativeConfig.getMaxWritesPerTransaction(), is(30));
        assertThat(nativeConfig.getRetentionPeriodDays(), is(4));
        assertThat(nativeConfig.isSkipArchiveLeaseCheck(), is(true));
        assertThat(nativeConfig.getLeaseDAOScanPageSize(), is(150));
        assertThat(nativeConfig.isFastCatchupModeEnabled(), is(true));
    }

    private static void assertBucketConfig(MeteringBucketConfig config,
                                           String bucketName,
                                           String serviceName,
                                           String meterName) {
        assertThat(config.bucketName(), is(bucketName));
        assertThat(config.serviceName(), is(serviceName));
        assertThat(config.meterName(), is(meterName));
    }

    private static Config config(Map<String, String> values) {
        return Config.just(ConfigSources.create(values));
    }
}
