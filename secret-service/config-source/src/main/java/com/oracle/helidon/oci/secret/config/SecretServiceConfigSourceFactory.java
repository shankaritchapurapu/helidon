/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.MetaConfig;
import io.helidon.config.spi.ConfigSource;
import io.helidon.service.registry.Service;

import static com.oracle.helidon.oci.secret.config.Ssv2Client.DEFAULT_ENDPOINT;

@Service.Singleton
class SecretServiceConfigSourceFactory {
    private static final String OCI_CONFIG_PATH = "oci-config.yaml";
    private static final String OCI_CONFIG_PREFIX = "helidon.oci-secret-service";
    private static final String OCI_ENV_CONFIG_SOURCE = "oci-env";

    private final Config config;
    private final Optional<ConfigSource> ociEnvConfigSource;

    @Service.Inject
    SecretServiceConfigSourceFactory(@Service.Named(SecretServiceConfigSource.TYPE) Optional<MetaConfig> metaConfig,
                                     @Service.Named(OCI_ENV_CONFIG_SOURCE) Optional<ConfigSource> ociEnvConfigSource) {
        this.config = Config.just(
                ConfigSources.create(overrides()),
                ConfigSources.create(metaConfig.map(MetaConfig::metaConfiguration)
                                             .orElseGet(SecretServiceConfigSourceFactory::ociConfig)),
                ociEnvConfigSource.orElseGet(ConfigSources::empty),
                ConfigSources.create(Map.of("client.endpoint", DEFAULT_ENDPOINT))
        );
        this.ociEnvConfigSource = Objects.requireNonNullElse(ociEnvConfigSource, Optional.empty());
    }

    SecretServiceConfigSourceBuilder builder() {
        return SecretServiceConfigSource.builder()
                .ociEnvConfigSource(ociEnvConfigSource)
                .config(config);
    }

    private static Config overrides() {
        return Config.just(
                        ConfigSources.environmentVariables(),
                        ConfigSources.systemProperties())
                .get("helidon.oci-secret-service");
    }

    private static Config ociConfig() {
        return Config.just(
                        ConfigSources.file(OCI_CONFIG_PATH).optional(true),
                        ConfigSources.classpath(OCI_CONFIG_PATH).optional(true)
                )
                .get(OCI_CONFIG_PREFIX);
    }
}
