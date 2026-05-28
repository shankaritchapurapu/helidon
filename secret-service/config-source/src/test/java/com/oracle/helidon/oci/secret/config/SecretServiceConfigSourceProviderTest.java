/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.MetaConfig;
import io.helidon.config.spi.ConfigSource;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class SecretServiceConfigSourceProviderTest {
    private ServiceRegistryManager registryManager;

    @AfterEach
    void shutdownServices() {
        if (registryManager != null) {
            registryManager.shutdown();
        }
    }

    @Test
    void loadsSecretServiceSourceThroughMetaConfig() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", SecretServiceConfigSource.TYPE),
                entry("properties.prefix", "custom.ssv2"),
                entry("properties.cache-ttl", "PT5S"),
                entry("properties.poll-interval", "PT10S"),
                entry("properties.client.enabled", "false"))));

        ConfigSource source = metaConfig.as(MetaConfig::configSource).get().getFirst();

        assertThat(source, instanceOf(SecretServiceConfigSource.class));
        assertThat(((SecretServiceConfigSource) source).uid(), is("custom.ssv2"));
    }

    @Test
    void providerPathUsesClasspathOciConfigForMissingProperties() {
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("type", SecretServiceConfigSource.TYPE))));

        ConfigSource source = metaConfig.as(MetaConfig::configSource).get().getFirst();

        assertThat(source, instanceOf(SecretServiceConfigSource.class));
        // The classpath oci-config.yaml fills missing provider settings.
        // Its prefix becomes the source UID.
        assertThat(((SecretServiceConfigSource) source).uid(), is("oci.file.ssv2"));
    }

    @Test
    void providerSourceResolvesDefaultEndpointFromLiveOciEnvSource() {
        registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());

        ConfigSource ociEnvSource = ConfigSources.create(Map.of("oci.env.iaas-domain-name", "r2.oracleiaas.com"),
                                                         "oci-env")
                .build();
        Services.setNamed(ConfigSource.class, ociEnvSource, "oci-env");

        SecretServiceConfigSourceBuilder builder = SecretServiceConfigSource.builder()
                .ociEnvConfigSource(SecretServiceConfigSourceProvider::ociEnvConfigSource)
                .config(Config.empty());

        assertThat(builder.resolvedClientConfig().endpoint(),
                   is("https://secret-service-ce.r2.oracleiaas.com/v1"));
    }
}
