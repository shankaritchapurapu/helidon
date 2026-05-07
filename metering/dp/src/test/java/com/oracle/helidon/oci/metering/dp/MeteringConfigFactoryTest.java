/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class MeteringConfigFactoryTest {

    @Test
    void loadsConfiguredValues() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.ofEntries(
                Map.entry("oci.metering.endpoint", "https://bling-dp.example"),
                Map.entry("oci.metering.metering-period", "PT90S"),
                Map.entry("oci.metering.archiving-duration", "PT2H"),
                Map.entry("oci.metering.metering-dir", "/var/metering"),
                Map.entry("oci.metering.client-id", "dp-client"),
                Map.entry("oci.metering.service", "dp-service"),
                Map.entry("oci.metering.os-enabled", "true"),
                Map.entry("oci.metering.k8s-based-deployment", "true"),
                Map.entry("oci.metering.bucket-name", "usage-bucket"),
                Map.entry("oci.metering.namespace", "metering-namespace"),
                Map.entry("oci.metering.report-to-bling-frequency", "7"),
                Map.entry("oci.metering.host-name", "dp-host")
        ))).get();

        assertThat(config.endpoint(), is("https://bling-dp.example"));
        assertThat(config.meteringPeriod(), is(Optional.of(Duration.ofSeconds(90))));
        assertThat(config.archivingDuration(), is(Optional.of(Duration.ofHours(2))));
        assertThat(config.meteringDir(), is(Optional.of("/var/metering")));
        assertThat(config.clientId(), is(Optional.of("dp-client")));
        assertThat(config.service(), is(Optional.of("dp-service")));
        assertThat(config.osEnabled(), is(Optional.of(true)));
        assertThat(config.k8sBasedDeployment(), is(Optional.of(true)));
        assertThat(config.bucketName(), is(Optional.of("usage-bucket")));
        assertThat(config.namespace(), is(Optional.of("metering-namespace")));
        assertThat(config.reportToBlingFrequency(), is(Optional.of(7)));
        assertThat(config.hostName(), is(Optional.of("dp-host")));
    }

    @Test
    void loadsOnlyRequiredEndpoint() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.of(
                "oci.metering.endpoint", "https://bling-dp.example"
        ))).get();

        assertThat(config.endpoint(), is("https://bling-dp.example"));
        assertThat(config.meteringPeriod(), is(Optional.empty()));
        assertThat(config.archivingDuration(), is(Optional.empty()));
        assertThat(config.meteringDir(), is(Optional.empty()));
        assertThat(config.clientId(), is(Optional.empty()));
        assertThat(config.service(), is(Optional.empty()));
        assertThat(config.osEnabled(), is(Optional.empty()));
        assertThat(config.k8sBasedDeployment(), is(Optional.empty()));
        assertThat(config.bucketName(), is(Optional.empty()));
        assertThat(config.namespace(), is(Optional.empty()));
        assertThat(config.reportToBlingFrequency(), is(Optional.empty()));
        assertThat(config.hostName(), is(Optional.empty()));
    }

    @Test
    void convertsToNativeConfig() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.ofEntries(
                Map.entry("oci.metering.endpoint", "https://bling-dp.example"),
                Map.entry("oci.metering.metering-period", "PT2M"),
                Map.entry("oci.metering.archiving-duration", "PT45M"),
                Map.entry("oci.metering.metering-dir", "/var/metering"),
                Map.entry("oci.metering.client-id", "dp-client"),
                Map.entry("oci.metering.service", "dp-service"),
                Map.entry("oci.metering.os-enabled", "true"),
                Map.entry("oci.metering.k8s-based-deployment", "true"),
                Map.entry("oci.metering.bucket-name", "usage-bucket"),
                Map.entry("oci.metering.namespace", "metering-namespace"),
                Map.entry("oci.metering.report-to-bling-frequency", "9")
        ))).get();

        com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig =
                new MeteringAgentConfigFactory(config).get();

        assertThat(nativeConfig.getEndpoint(), is("https://bling-dp.example"));
        assertThat(nativeConfig.getMeteringPeriodInSeconds(), is(120));
        assertThat(nativeConfig.getArchivingDurationInSeconds(), is(2700));
        assertThat(nativeConfig.getMeteringDir(), is("/var/metering"));
        assertThat(nativeConfig.getClientId(), is("dp-client"));
        assertThat(nativeConfig.getService(), is("dp-service"));
        assertThat(nativeConfig.isOsEnabled(), is(true));
        assertThat(nativeConfig.isK8sBasedDeployment(), is(true));
        assertThat(nativeConfig.getBucketName(), is("usage-bucket"));
        assertThat(nativeConfig.getNamespace(), is("metering-namespace"));
        assertThat(nativeConfig.getReportToBlingFrequency(), is(9));
    }

    private static Config config(Map<String, String> values) {
        return Config.just(ConfigSources.create(values));
    }
}
