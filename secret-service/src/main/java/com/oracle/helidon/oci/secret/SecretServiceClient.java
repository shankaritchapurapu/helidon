/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigParser;
import io.helidon.config.yaml.YamlConfigParser;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistry;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.vault.SecretServiceConfig;
import com.oracle.pic.vault.VaultClient;
import org.bouncycastle.util.encoders.Base64;

import static java.lang.System.Logger.Level.DEBUG;
import static java.nio.charset.StandardCharsets.UTF_8;

class SecretServiceClient {

    static final String DEFAULT_PREFIX = "oci.ssv2";
    private static final System.Logger LOGGER = System.getLogger(SecretServiceClient.class.getName());
    private static final String REGION_MASK = "<<region>>";
    private static final String DEF_CONF = """
            prefix: oci.ssv2
            endpoint: "https://secret-service-ce.<<region>>.oracleiaas.com/v1"
            tlsConfig.caBundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
            cacheConfig:
              cacheType: IN_MEMORY_CACHE
            retryConfig:
              maxRetries: 3
            """;

    private final LazyValue<VaultClient> secretsClient;
    private final SecretServiceConfig secretServiceConfig;
    private final Config config;
    private final Boolean enabled;

    private SecretServiceClient(Config metaConfig) {
        this.config = prepareConfig(metaConfig);
        this.enabled = config.get("enabled").asBoolean().orElse(Boolean.TRUE);
        this.secretServiceConfig = config.as(SecretServiceConfig.class).orElseThrow();
        this.secretsClient = LazyValue.create(this::initClient);
    }

    static SecretServiceClient create(Config config) {
        return new SecretServiceClient(config);
    }

    VaultClient initClient() {
        ServiceRegistry registry = GlobalServiceRegistry.registry();
        var provider = registry.get(BasicAuthenticationDetailsProvider.class);
        var region = registry.get(Region.class);

        resolveEndpoint(region.getRegionCode());
        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "Initializing vault client with configuration: " + secretServiceConfig);
        }
        return new VaultClient(secretServiceConfig, provider);
    }

    void resolveEndpoint(String region) {
        var endpoint = secretServiceConfig.getEndpoint();
        secretServiceConfig.setEndpoint(endpoint.replaceAll(REGION_MASK, region));
    }

    Optional<byte[]> getSecret(String path) {
        if (!enabled) {
            return Optional.empty();
        }

        String base64 = this.secretsClient.get()
                .getSecret(path)
                .getData()
                .get("secret");

        return Optional.of(Base64.decode(base64));
    }

    SecretServiceConfig getSecretServiceConfig() {
        return secretServiceConfig;
    }

    private Config prepareConfig(Config metaConfig) {
        ConfigNode.ObjectNode defaultConfig = YamlConfigParser.create()
                .parse(ConfigParser.Content.builder()
                               .data(new ByteArrayInputStream(DEF_CONF.getBytes(UTF_8)))
                               .charset(UTF_8)
                               .build());
        return Config.builder()
                .addSource(ConfigSources.create(metaConfig))
                .addSource(ConfigSources.create(defaultConfig))
                .build();
    }
}
