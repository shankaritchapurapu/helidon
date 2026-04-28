/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;
import io.helidon.config.MetaConfig;
import io.helidon.config.spi.ConfigSource;

import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        ConfigSource source = metaConfig.as(MetaConfig::configSource).get().getFirst();
        Config config = Config.builder(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        assertThat(config.get("test.env.region").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(config.get("test.env.availability-domain").asString().orElseThrow(), is("iad-ad-1"));
        assertThat(config.get("test.env.fault-domain").asString().orElseThrow(), is("2"));
    }

    @Test
    void keepsProviderPathLazyUntilValueAccess() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", OciEnvConfigSourceProvider.TYPE),
                entry("properties.dynamic-core-regions.enabled", "false"),
                entry("properties.location-override.region", "not-a-region"),
                entry("properties.location-override.availability-domain", "iad-ad-1"))));

        ConfigSource source = metaConfig.as(MetaConfig::configSource).get().getFirst();
        Config config = Config.builder(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        ConfigException exception = assertThrows(ConfigException.class,
                                                 () -> config.get("oci.env.region").asString().orElseThrow());

        assertThat(exception.getMessage(), is("Configured location override region 'not-a-region' is invalid"));
    }
}
