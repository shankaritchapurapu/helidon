/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.MetaConfig;
import io.helidon.config.spi.ConfigSource;

import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class OciEnvConfigSourceProviderTest {

    @Test
    void loadsOciEnvSourceThroughMetaConfig() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", OciEnvConfigSourceProvider.TYPE),
                entry("properties.dynamic-core-regions.enabled", "false"),
                entry("properties.prefix", "test.env"),
                entry("properties.location-override.region", "us-ashburn-1"),
                entry("properties.location-override.availability-domain", "iad-ad-1"),
                entry("properties.location-override.fault-domain", "2"))));

        Config config = providerConfig(metaConfig);

        // All values come from explicit provider properties.
        assertThat(config.get("test.env.region").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(config.get("test.env.availability-domain").asString().orElseThrow(), is("iad-ad-1"));
        assertThat(config.get("test.env.fault-domain").asString().orElseThrow(), is("2"));
    }

    @Test
    void providerPathUsesClasspathOciConfigForMissingProperties() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", OciEnvConfigSourceProvider.TYPE))));

        Config config = providerConfig(metaConfig);

        // With no provider properties, all configured values come from classpath oci-config.yaml.
        assertThat(config.get("oci.file.region").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(config.get("oci.file.availability-domain").asString().orElseThrow(), is("iad-ad-1"));
        assertThat(config.get("oci.file.fault-domain").asString().orElseThrow(), is("5"));
    }

    @Test
    void providerPathOverridesOciConfigPerKey() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", OciEnvConfigSourceProvider.TYPE),
                entry("properties.prefix", "provider.env"),
                entry("properties.location-override.fault-domain", "2"))));

        Config config = providerConfig(metaConfig);

        // Provider properties select the published prefix and fault domain.
        // Missing region and availability-domain fall back to classpath oci-config.yaml.
        assertThat(config.get("provider.env.region").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(config.get("provider.env.availability-domain").asString().orElseThrow(), is("iad-ad-1"));
        assertThat(config.get("provider.env.fault-domain").asString().orElseThrow(), is("2"));
    }

    @Test
    void providerPathUsesProviderValuesWhenAllKeysAreConfigured() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", OciEnvConfigSourceProvider.TYPE),
                entry("properties.dynamic-core-regions.enabled", "false"),
                entry("properties.prefix", "provider.only"),
                entry("properties.location-override.region", "eu-frankfurt-1"),
                entry("properties.location-override.availability-domain", "eu-frankfurt-1-ad-1"),
                entry("properties.location-override.fault-domain", "3"))));

        Config config = providerConfig(metaConfig);

        // Every overlapping key is supplied by provider properties, so oci-config.yaml cannot
        // redirect values back to the fallback prefix or location.
        assertThat(config.get("provider.only.region").asString().orElseThrow(), is("eu-frankfurt-1"));
        assertThat(config.get("provider.only.availability-domain").asString().orElseThrow(), is("eu-frankfurt-1-ad-1"));
        assertThat(config.get("provider.only.fault-domain").asString().orElseThrow(), is("3"));
        assertThat(config.get("oci.file.region").exists(), is(false));
    }

    @Test
    void returnsEmptyValuesWhenProviderPathMaterializationFails() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", OciEnvConfigSourceProvider.TYPE),
                entry("properties.dynamic-core-regions.enabled", "false"),
                entry("properties.prefix", "oci.env"),
                entry("properties.location-override.region", "not-a-region"),
                entry("properties.location-override.availability-domain", "iad-ad-1"))));

        Config config = providerConfig(metaConfig);

        assertThat(config.get("oci.env.region").exists(), is(false));
        assertThat(config.get("oci.env.availability-domain").exists(), is(false));
    }

    @Test
    void returnsEmptyValuesWhenProviderPathConfigConversionFails() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", OciEnvConfigSourceProvider.TYPE),
                entry("properties.dynamic-core-regions.enabled", "false"),
                entry("properties.prefix", "oci.env"),
                entry("properties.location-override.region", "us-ashburn-1"),
                entry("properties.location-override.availability-domain", "iad-ad-1"),
                entry("properties.location-override.fault-domain", "bogus"))));

        Config config = providerConfig(metaConfig);

        assertThat(config.get("oci.env.region").exists(), is(false));
        assertThat(config.get("oci.env.availability-domain").exists(), is(false));
    }

    private static Config providerConfig(Config metaConfig) {
        ConfigSource source = metaConfig.as(MetaConfig::configSource).get().getFirst();
        return Config.builder(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();
    }
}
