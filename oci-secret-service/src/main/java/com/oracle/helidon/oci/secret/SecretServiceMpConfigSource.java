/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.secret;

import java.io.ByteArrayInputStream;
import java.lang.System.Logger;
import java.util.Optional;
import java.util.Set;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigParser;
import io.helidon.config.yaml.YamlConfigParser;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.pic.vault.SecretServiceConfig;
import com.oracle.pic.vault.VaultClient;
import jakarta.annotation.Priority;
import org.bouncycastle.util.encoders.Base64;
import org.eclipse.microprofile.config.spi.ConfigSource;

import static java.lang.System.Logger.Level.DEBUG;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Only properties with prefix configured in meta config are evaluated.
 * Example:
 * <pre>{@code
 * sources:
 *   - type: 'environment-variables'
 *   - type: 'system-properties'
 *   - type: 'oci-secret-service'
 *     # Optional config
 *     prefix: oci.ssv2
 *     endpoint: "https://secret-service-ce.<<region>>.oracleiaas.com/v1"
 *     tlsConfig.caBundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
 *     cacheConfig:
 *       cacheType: IN_MEMORY_CACHE
 *       cacheExpiryInSeconds: 10
 *       cacheRefreshIntervalInSeconds: 2
 *     retryConfig:
 *       maxRetries: 3
 *       minRetryDelayInMs: 100
 *     authProvider:
 *       timeout: 500
 *       retries: 8
 *
 * }</pre>
 */
@Priority(5000)
public class SecretServiceMpConfigSource implements ConfigSource {

    private static final Logger LOGGER = System.getLogger(SecretServiceMpConfigSource.class.getName());

    private static final String REGION_MASK = "<<region>>";
    private static final String DEF_CONF = """
            prefix: oci.ssv2
            endpoint: "https://secret-service-ce.<<region>>.oracleiaas.com/v1"
            tlsConfig.caBundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
            cacheConfig:
              cacheType: IN_MEMORY_CACHE
            retryConfig:
              maxRetries: 3
            authProvider:
              timeout: 500
              retries: 8
            """;

    private final String prefix;
    private final LazyValue<VaultClient> secretsClient;
    private final SecretServiceConfig secretServiceConfig;
    private final int ordinal;
    private final Config config;
    private final AuthProviderConfig authProviderConfig;

    public SecretServiceMpConfigSource() {
        this.config = prepareConfig(Config.empty());
        this.ordinal = 200;
        this.prefix = config.get("prefix").asString().orElseThrow();
        this.secretServiceConfig = this.config.as(SecretServiceConfig.class).orElseThrow();
        this.authProviderConfig = config.get("authProvider").map(AuthProviderConfig::create).orElseThrow();
        this.secretsClient = LazyValue.create(this::initClient);
    }

    SecretServiceMpConfigSource(Config metaConfig, int ordinal) {
        this.config = prepareConfig(metaConfig);
        this.ordinal = ordinal;
        this.prefix = config.get("prefix").asString().orElseThrow();
        this.secretServiceConfig = config.as(SecretServiceConfig.class).orElseThrow();
        this.authProviderConfig = config.get("authProvider").map(AuthProviderConfig::create).orElseThrow();
        this.secretsClient = LazyValue.create(this::initClient);
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getValue(String propertyName) {
        Prop prop = parse(propertyName);
        if (!prop.hasPrefix() || prop.name().isBlank()) {
            return null;
        }

        String base64 = this.secretsClient.get()
                .getSecret(prop.path())
                .getData()
                .get("secret");

        return new String(Base64.decode(base64), UTF_8);
    }

    @Override
    public String getName() {
        return "oci-secret-service";
    }

    VaultClient initClient() {
        var provider = InstancePrincipalsAuthenticationDetailsProvider.builder()
                .timeoutForEachRetry(Math.toIntExact(authProviderConfig.timeout()))
                .detectEndpointRetries(authProviderConfig.retries())
                .build();
        var region = provider.getRegion().getRegionId();
        resolveEndpoint(region);
        LOGGER.log(DEBUG, "Initializing vault client with configuration: " + secretServiceConfig);
        return new VaultClient(secretServiceConfig, provider);
    }

    Prop parse(String propName) {
        // %PROFILE.PREFIX.NAME
        // %TEST.oci.vault.helidon1.test-kec
        String rawName;
        Optional<String> profile;
        if (propName.startsWith("%")) {
            int firstDotIdx = propName.indexOf('.');
            profile = Optional.of(propName.substring(1, firstDotIdx));
            rawName = propName.substring(firstDotIdx + 1);
        } else {
            profile = Optional.empty();
            rawName = propName;
        }

        boolean hasPrefix = rawName.startsWith(prefix);
        String path = null;
        String name;
        if (hasPrefix && prefix.length() != rawName.length()) {
            name = rawName.substring(prefix.length() + 1);
            path = "/" + name.replaceAll("\\.", "/");
        } else {
            name = rawName;
        }
        return new Prop(profile,
                        hasPrefix,
                        name,
                        path);
    }

    SecretServiceConfig getSecretServiceConfig() {
        return secretServiceConfig;
    }

    void resolveEndpoint(String region) {
        var endpoint = secretServiceConfig.getEndpoint();
        secretServiceConfig.setEndpoint(endpoint.replaceAll(REGION_MASK, region));
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

    record Prop(Optional<String> profile, boolean hasPrefix, String name, String path) {
    }

    record AuthProviderConfig(long timeout, int retries) {
        static AuthProviderConfig create(io.helidon.common.config.Config c) {
            return new AuthProviderConfig(c.get("timeout").asLong().orElseThrow(),
                                          c.get("retries").asInt().orElseThrow());
        }
    }
}
