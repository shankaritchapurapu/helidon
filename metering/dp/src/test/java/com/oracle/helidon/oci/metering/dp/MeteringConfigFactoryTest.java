/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.Services;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeteringConfigFactoryTest {

    @BeforeAll
    static void setUpRegistry() {
        Services.set(BasicAuthenticationDetailsProvider.class, authProvider());
    }

    @Test
    void loadsConfiguredValues() {
        Config helidonConfig = config(Map.ofEntries(
                Map.entry("oci.metering.endpoint", "https://bling-dp.example"),
                Map.entry("oci.metering.enabled", "false"),
                Map.entry("oci.metering.metering-period", "PT90S"),
                Map.entry("oci.metering.archiving-duration", "PT2H"),
                Map.entry("oci.metering.metering-dir", "/var/metering"),
                Map.entry("oci.metering.client-id", "dp-client"),
                Map.entry("oci.metering.service", "dp-service"),
                Map.entry("oci.metering.os-enabled", "true"),
                Map.entry("oci.metering.k8s-based-deployment", "true"),
                Map.entry("oci.metering.bucket-name", "usage-bucket"),
                Map.entry("oci.metering.namespace", "metering-namespace"),
                Map.entry("oci.metering.report-interval", "PT7S"),
                Map.entry("oci.metering.region", "us-phoenix-1"),
                Map.entry("oci.metering.host-name", "dp-host"),
                Map.entry("oci.metering.bling-publisher-client.endpoint", "https://bling-client.example"),
                Map.entry("oci.metering.bling-publisher-client.client-id", "publisher-client")
        ));
        MeteringConfig config = new MeteringConfigFactory(helidonConfig).get();
        BlingPublisherClientConfig blingConfig =
                BlingPublisherClientConfig.create(helidonConfig.get("oci.metering.bling-publisher-client"));

        assertThat(config.endpoint(), is("https://bling-dp.example"));
        assertThat(config.enabled(), is(false));
        assertThat(config.meteringPeriod(), is(Optional.of(Duration.ofSeconds(90))));
        assertThat(config.archivingDuration(), is(Optional.of(Duration.ofHours(2))));
        assertThat(config.meteringDir(), is("/var/metering"));
        assertThat(config.clientId(), is("dp-client"));
        assertThat(config.service(), is("dp-service"));
        assertThat(config.osEnabled(), is(Optional.of(true)));
        assertThat(config.k8sBasedDeployment(), is(Optional.of(true)));
        assertThat(config.bucketName(), is(Optional.of("usage-bucket")));
        assertThat(config.namespace(), is(Optional.of("metering-namespace")));
        assertThat(config.reportInterval(), is(Optional.of(Duration.ofSeconds(7))));
        assertThat(config.region(), is(Optional.of("us-phoenix-1")));
        assertThat(config.hostName(), is(Optional.of("dp-host")));
        assertThat(blingConfig.endpoint(), is("https://bling-client.example"));
        assertThat(blingConfig.clientId(), is("publisher-client"));
    }

    @Test
    void loadsRequiredBlueprintInputsButNativeValidationStillApplies() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.of(
                "oci.metering.endpoint", "https://bling-dp.example",
                "oci.metering.metering-dir", "/var/metering",
                "oci.metering.client-id", "dp-client",
                "oci.metering.service", "dp-service",
                "oci.metering.bling-publisher-client.endpoint", "https://bling-client.example",
                "oci.metering.bling-publisher-client.client-id", "publisher-client"
        ))).get();

        assertThat(config.endpoint(), is("https://bling-dp.example"));
        assertThat(config.enabled(), is(true));
        assertThat(config.meteringPeriod(), is(Optional.empty()));
        assertThat(config.archivingDuration(), is(Optional.empty()));
        assertThat(config.meteringDir(), is("/var/metering"));
        assertThat(config.clientId(), is("dp-client"));
        assertThat(config.service(), is("dp-service"));
        assertThat(config.osEnabled(), is(Optional.empty()));
        assertThat(config.k8sBasedDeployment(), is(Optional.empty()));
        assertThat(config.bucketName(), is(Optional.empty()));
        assertThat(config.namespace(), is(Optional.empty()));
        assertThat(config.reportInterval(), is(Optional.empty()));
        assertThat(config.region(), is(Optional.empty()));
        assertThat(config.hostName(), is(Optional.empty()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> new MeteringAgentConfigFactory(config).get());

        assertThat(exception.getMessage(), containsString("Object Storage Backup is enabled"));
    }

    @Test
    void convertsMinimalNoObjectStorageBackupConfigToNativeConfig() {
        MeteringConfig config = new MeteringConfigFactory(config(Map.of(
                "oci.metering.endpoint", "https://bling-dp.example",
                "oci.metering.metering-dir", "/var/metering",
                "oci.metering.client-id", "dp-client",
                "oci.metering.service", "dp-service",
                "oci.metering.os-enabled", "false",
                "oci.metering.k8s-based-deployment", "false",
                "oci.metering.bling-publisher-client.endpoint", "https://bling-client.example",
                "oci.metering.bling-publisher-client.client-id", "publisher-client"
        ))).get();

        com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig =
                new MeteringAgentConfigFactory(config).get();

        assertThat(nativeConfig.getEndpoint(), is("https://bling-dp.example"));
        assertThat(nativeConfig.getMeteringDir(), is("/var/metering"));
        assertThat(nativeConfig.getClientId(), is("dp-client"));
        assertThat(nativeConfig.getService(), is("dp-service"));
        assertThat(nativeConfig.isOsEnabled(), is(false));
        assertThat(nativeConfig.isK8sBasedDeployment(), is(false));
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
                Map.entry("oci.metering.report-interval", "PT9S"),
                Map.entry("oci.metering.region", "us-phoenix-1"),
                Map.entry("oci.metering.bling-publisher-client.endpoint", "https://bling-client.example"),
                Map.entry("oci.metering.bling-publisher-client.client-id", "publisher-client")
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

    @Test
    void rejectsSubSecondReportInterval() {
        ConfigException exception = assertThrows(
                ConfigException.class,
                () -> new MeteringConfigFactory(config(Map.of(
                        "oci.metering.endpoint", "https://bling-dp.example",
                        "oci.metering.metering-dir", "/var/metering",
                        "oci.metering.client-id", "dp-client",
                        "oci.metering.service", "dp-service",
                        "oci.metering.report-interval", "PT0.5S",
                        "oci.metering.bling-publisher-client.endpoint", "https://bling-client.example",
                        "oci.metering.bling-publisher-client.client-id", "publisher-client"
                ))).get());

        assertThat(exception.getMessage(), containsString("at least PT1S"));
    }

    @Test
    void rejectsMeteringPeriodWithSubSecondPrecision() {
        MeteringConfig config = meteringConfigWithDuration("metering-period", "PT1.5S");

        assertThrows(IllegalArgumentException.class, () -> new MeteringAgentConfigFactory(config).get());
    }

    @Test
    void rejectsArchivingDurationWithSubSecondPrecision() {
        MeteringConfig config = meteringConfigWithDuration("archiving-duration", "PT1.5S");

        assertThrows(IllegalArgumentException.class, () -> new MeteringAgentConfigFactory(config).get());
    }

    @Test
    void rejectsReportIntervalWithSubSecondPrecision() {
        MeteringConfig config = meteringConfigWithDuration("report-interval", "PT1.5S");

        assertThrows(IllegalArgumentException.class, () -> new MeteringAgentConfigFactory(config).get());
    }

    private static MeteringConfig meteringConfigWithDuration(String key, String value) {
        Map<String, String> values = new java.util.HashMap<>(Map.of(
                "oci.metering.endpoint", "https://bling-dp.example",
                "oci.metering.metering-dir", "/var/metering",
                "oci.metering.client-id", "dp-client",
                "oci.metering.service", "dp-service",
                "oci.metering.os-enabled", "false",
                "oci.metering.k8s-based-deployment", "false",
                "oci.metering.bling-publisher-client.endpoint", "https://bling-client.example",
                "oci.metering.bling-publisher-client.client-id", "publisher-client"
        ));
        values.put("oci.metering." + key, value);
        return new MeteringConfigFactory(config(values)).get();
    }

    private static Config config(Map<String, String> values) {
        return Config.just(ConfigSources.create(values));
    }

    private static BasicAuthenticationDetailsProvider authProvider() {
        Path keyPath = Path.of("..", "..", "limits", "src", "test", "resources", "key.pem");
        return SimpleAuthenticationDetailsProvider.builder()
                .tenantId("ocid1.tenancy.oc1..testtenant")
                .userId("ocid1.user.oc1..testuser")
                .fingerprint("11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:00")
                .region(Region.US_ASHBURN_1)
                .privateKeySupplier(() -> {
                    try {
                        return Files.newInputStream(keyPath);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to read test private key", e);
                    }
                })
                .build();
    }
}
