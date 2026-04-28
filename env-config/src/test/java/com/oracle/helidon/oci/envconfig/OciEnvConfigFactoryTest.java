/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.oracle.pic.commons.util.AvailabilityDomain;
import com.oracle.pic.commons.util.Region;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;
import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

class OciEnvConfigFactoryTest {

    @Test
    void createsPropertiesFromLocationOverrides() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("prefix", "custom.prefix"),
                entry("dynamic-core-regions.enabled", "false"),
                entry("location-override.region", "us-ashburn-1"),
                entry("location-override.availability-domain", "iad-ad-2"),
                entry("location-override.fault-domain", "3"))));

        Config values = config(new OciEnvConfigFactory(config));

        assertThat(values.get("custom.prefix.realm").asString().orElseThrow(), is("oc1"));
        assertThat(values.get("custom.prefix.region").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(values.get("custom.prefix.region-name").asString().orElseThrow(), is("iad"));
        assertThat(values.get("custom.prefix.region-internal-name").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(values.get("custom.prefix.availability-domain").asString().orElseThrow(), is("iad-ad-2"));
        assertThat(values.get("custom.prefix.fault-domain").asString().orElseThrow(), is("3"));
        assertThat(values.get("custom.prefix.public-domain-name").asString().orElseThrow(), is("us-ashburn-1.oraclecloud.com"));
        assertThat(values.get("custom.prefix.realm-public-domain-name").asString().orElseThrow(), is("oraclecloud.com"));
        assertThat(values.get("custom.prefix.oci-public-domain-name").asString().orElseThrow(), is("us-ashburn-1.oci.oraclecloud.com"));
        assertThat(values.get("custom.prefix.iaas-domain-name").asString().orElseThrow(), is("us-ashburn-1.oracleiaas.com"));
        assertThat(values.get("custom.prefix.realm-iaas-domain-name").asString().orElseThrow(), is("oracleiaas.com"));
        assertThat(values.get("custom.prefix.oci-iaas-domain-name").asString().orElseThrow(), is("us-ashburn-1.oci.oracleiaas.com"));
        assertThat(values.get("custom.prefix.first-region-in-realm-public-name").asString().orElseThrow(),
                   is(Region.fromPublicRegionName("us-ashburn-1").getRealm().getAttributes().getFirstRegion()));
        assertThat(values.get("custom.prefix.ad-number").asString().orElseThrow(), is("ad2"));
        assertThat(values.get("custom.prefix.number-for-ad").asString().orElseThrow(), is("2"));
        assertThat(values.get("custom.prefix.airport-code").asString().orElseThrow(), is("IAD"));
        assertThat(values.get("custom.prefix.db-tns-name").asString().orElseThrow(), is("usashburn1"));
    }

    @Test
    void acceptsEnumStyleLocationOverrides() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("prefix", "custom.prefix"),
                entry("dynamic-core-regions.enabled", "false"),
                entry("location-override.region", "AF_JOHANNESBURG_1"),
                entry("location-override.availability-domain", "AF_JOHANNESBURG_1_AD_1"))));

        Config values = config(new OciEnvConfigFactory(config));

        assertThat(values.get("custom.prefix.region").asString().orElseThrow(), is(Region.AF_JOHANNESBURG_1.getPublicRegionName()));
        assertThat(values.get("custom.prefix.region-name").asString().orElseThrow(), is(Region.AF_JOHANNESBURG_1.getName()));
        assertThat(values.get("custom.prefix.availability-domain").asString().orElseThrow(),
                   is(AvailabilityDomain.AF_JOHANNESBURG_1_AD_1.getName()));
    }

    @Test
    void resolvesRegionIdFromLocationOverrideWithoutAvailabilityDomain() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.enabled", "false"),
                entry("location-override.region", "us-ashburn-1"))));

        OciEnvConfigFactory factory = mockFactory(config);

        assertThat(factory.regionId().orElseThrow(), is("us-ashburn-1"));
        verify(factory, never()).readFileValue(OciEnvConfigFactory.DEFAULT_AVAILABILITY_DOMAIN_PATH);
    }

    @Test
    void returnsEmptyRegionIdWhenRegionFileMissing() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.enabled", "false"))));
        OciEnvConfigFactory factory = mockFactory(config);
        doReturn(Optional.empty()).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_REGION_PATH);

        assertThat(factory.regionId().isEmpty(), is(true));
        verify(factory, never()).readFileValue(OciEnvConfigFactory.DEFAULT_AVAILABILITY_DOMAIN_PATH);
    }

    @Test
    void readsLocationFromEnvironmentFiles() {
        OciEnvConfigFactory factory = mockFactory(Config.empty());
        doReturn(Optional.of("eu-frankfurt-1")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_REGION_PATH);
        doReturn(Optional.of("ad1")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_AVAILABILITY_DOMAIN_PATH);
        doReturn(Optional.of("2")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_FAULT_DOMAIN_PATH);

        Config values = config(factory);

        assertThat(values.get("oci.env.region").asString().orElseThrow(), is("eu-frankfurt-1"));
        assertThat(values.get("oci.env.region-name").asString().orElseThrow(), is("eu-frankfurt-1"));
        assertThat(values.get("oci.env.region-internal-name").asString().orElseThrow(), is("eu-frankfurt-1"));
        assertThat(values.get("oci.env.availability-domain").asString().orElseThrow(), is("eu-frankfurt-1-ad-1"));
        assertThat(values.get("oci.env.fault-domain").asString().orElseThrow(), is("2"));
        assertThat(values.get("oci.env.ad-number").asString().orElseThrow(), is("ad1"));
        assertThat(values.get("oci.env.number-for-ad").asString().orElseThrow(), is("1"));
    }

    @Test
    void readsPhysicalAvailabilityDomainWhenConfigured() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.enabled", "false"),
                entry("use-physical-availability-domain", "true"))));
        OciEnvConfigFactory factory = mockFactory(config);
        doReturn(Optional.of("eu-frankfurt-1")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_REGION_PATH);
        doReturn(Optional.of("ad2")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_PHYSICAL_AVAILABILITY_DOMAIN_PATH);
        doReturn(Optional.empty()).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_FAULT_DOMAIN_PATH);

        Config values = config(factory);

        assertThat(values.get("oci.env.availability-domain").asString().orElseThrow(), is("eu-frankfurt-1-ad-2"));
        assertThat(values.get("oci.env.ad-number").asString().orElseThrow(), is("ad2"));
        assertThat(values.get("oci.env.number-for-ad").asString().orElseThrow(), is("2"));
        verify(factory).readFileValue(OciEnvConfigFactory.DEFAULT_PHYSICAL_AVAILABILITY_DOMAIN_PATH);
        verify(factory, never()).readFileValue(OciEnvConfigFactory.DEFAULT_AVAILABILITY_DOMAIN_PATH);
    }

    @Test
    void acceptsOutOfRangeFaultDomainFromEnvironmentFile() {
        OciEnvConfigFactory factory = mockFactory(Config.empty());
        doReturn(Optional.of("eu-frankfurt-1")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_REGION_PATH);
        doReturn(Optional.of("ad1")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_AVAILABILITY_DOMAIN_PATH);
        doReturn(Optional.of("7")).when(factory).readFileValue(OciEnvConfigFactory.DEFAULT_FAULT_DOMAIN_PATH);

        Config values = config(factory);

        assertThat(values.get("oci.env.fault-domain").asString().orElseThrow(), is("7"));
    }

    @Test
    void overridesLocationToDevWhenConfigured() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.enabled", "false"),
                entry("location-override.region", "us-ashburn-1"),
                entry("location-override.availability-domain", "iad-ad-1"),
                entry("location-override-dev", "true"))));

        OciEnvConfigFactory factory = mockFactory(config);

        Config values = config(factory);

        assertThat(values.get("oci.env.region").asString().orElseThrow(), is(Region.DEV.getPublicRegionName()));
        assertThat(values.get("oci.env.availability-domain").asString().orElseThrow(), is(AvailabilityDomain.DEV_1.getName()));
        verify(factory, never()).readFileValue(any(Path.class));
    }

    @Test
    void acceptsOutOfRangeFaultDomainOverride() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.enabled", "false"),
                entry("location-override.region", "us-ashburn-1"),
                entry("location-override.availability-domain", "iad-ad-1"),
                entry("location-override.fault-domain", "7"))));

        Config values = config(new OciEnvConfigFactory(config));

        assertThat(values.get("oci.env.fault-domain").asString().orElseThrow(), is("7"));
    }

    private static Config config(OciEnvConfigFactory factory) {
        return Config.builder(ConfigSources.create(factory.create()))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();
    }

    private static OciEnvConfigFactory mockFactory(Config config) {
        return mock(OciEnvConfigFactory.class,
                    withSettings().useConstructor(config).defaultAnswer(CALLS_REAL_METHODS));
    }
}
