/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.MetaConfig;
import io.helidon.config.spi.ConfigSource;
import io.helidon.service.registry.Service;

import static com.oracle.helidon.oci.secret.config.DefaultSsv2Client.DEFAULT_ENDPOINT;

@Service.Singleton
class SecretServiceConfigSourceFactory {
    private static final String OCI_CONFIG_PATH = "oci-config.yaml";
    private static final String OCI_CONFIG_PREFIX = "helidon.oci-secret-service";
    private static final String OCI_ENV_CONFIG_SOURCE = "oci-env";

    private final Config config;
    private final Supplier<Optional<ConfigSource>> ociEnvConfigSource;
    private final Supplier<Ssv2Client> ssv2Client;

    @Service.Inject
    SecretServiceConfigSourceFactory(@Service.Named(SecretServiceConfigSource.TYPE) Optional<MetaConfig> metaConfig,
                                     @Service.Named(OCI_ENV_CONFIG_SOURCE) Supplier<Optional<ConfigSource>> ociEnvConfigSource,
                                     Supplier<Ssv2Client> ssv2Client) {
        this.ociEnvConfigSource = Objects.requireNonNull(ociEnvConfigSource, "ociEnvConfigSource");
        this.ssv2Client = Objects.requireNonNull(ssv2Client, "ssv2Client");
        this.config = config(metaConfig);
    }

    SecretServiceConfigSourceFactory(Optional<MetaConfig> metaConfig,
                                     Supplier<Optional<ConfigSource>> ociEnvConfigSource) {
        this(metaConfig,
             ociEnvConfigSource,
             () -> {
                 throw new IllegalStateException("SSv2 client lookup is not configured");
             });
    }

    SecretServiceConfigSourceBuilder builder() {
        return builder(config, ociEnvConfigSource)
                .resolver(path -> ssv2Client.get()
                        .getSecretAsBytes(path)
                        .map(value -> new String(value, StandardCharsets.UTF_8)));
    }

    Ssv2ClientConfig resolvedClientConfig() {
        return builder(config, ociEnvConfigSource)
                .resolvedClientConfig();
    }

    static SecretServiceConfigSourceBuilder builder(Config config,
                                                    Supplier<Optional<ConfigSource>> ociEnvConfigSource) {
        return SecretServiceConfigSource.builder()
                .ociEnvConfigSource(ociEnvConfigSource)
                .config(config);
    }

    static Config config(Optional<MetaConfig> metaConfig) {
        return Config.just(
                ConfigSources.create(overrides()),
                ConfigSources.create(serviceConfig(metaConfig)),
                ConfigSources.create(Map.of("client.endpoint", DEFAULT_ENDPOINT))
        );
    }

    /**
     * Builds provider-path config with provider/meta-config properties first
     * and {@code oci-config.yaml} second.
     * <p>
     * Helidon uses the first source that contains a requested key, so provider values override per key
     * and the file-backed config fills gaps.
     *
     * @param metaConfig provider/meta-config properties
     * @return merged provider-path config
     */
    static Config providerConfig(Config metaConfig) {
        Objects.requireNonNull(metaConfig, "metaConfig");
        return Config.just(ConfigSources.create(metaConfig),
                           ConfigSources.create(ociConfig()));
    }

    private static Config overrides() {
        return Config.just(
                        ConfigSources.environmentVariables(),
                        ConfigSources.systemProperties())
                .get("helidon.oci-secret-service");
    }

    private static Config serviceConfig(Optional<MetaConfig> metaConfig) {
        // Named provider config takes the overlay path so explicit provider keys win per key;
        // without it, the source reads helidon.oci-secret-service from oci-config.yaml directly.
        return metaConfig.map(MetaConfig::metaConfiguration)
                .map(SecretServiceConfigSourceFactory::providerConfig)
                .orElseGet(SecretServiceConfigSourceFactory::ociConfig);
    }

    private static Config ociConfig() {
        return Config.just(
                        ConfigSources.file(OCI_CONFIG_PATH).optional(true),
                        ConfigSources.classpath(OCI_CONFIG_PATH).optional(true)
                )
                .get(OCI_CONFIG_PREFIX);
    }
}
