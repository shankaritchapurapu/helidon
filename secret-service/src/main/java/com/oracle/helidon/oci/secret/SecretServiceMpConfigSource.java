/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.lang.System.Logger;
import java.util.Optional;
import java.util.Set;

import io.helidon.config.Config;

import jakarta.annotation.Priority;
import org.eclipse.microprofile.config.spi.ConfigSource;

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
 *
 * }</pre>
 */
@Priority(5000)
public class SecretServiceMpConfigSource implements ConfigSource {
    static final String DEFAULT_PREFIX = "oci.ssv2";
    private static final Logger LOGGER = System.getLogger(SecretServiceMpConfigSource.class.getName());
    private static SecretServiceClient defaultSecretServiceClient;
    private final String prefix;
    private final int ordinal;
    private SecretServiceClient secretServiceClient;

    /**
     * Constructor required as this may be loaded via {@link java.util.ServiceLoader}.
     */
    public SecretServiceMpConfigSource() {
        this.ordinal = 83;
        this.prefix = SecretServiceClient.DEFAULT_PREFIX;
        // Meta-configured default wins
        if (defaultSecretServiceClient == null) {
            defaultSecretServiceClient = SecretServiceClient.create(Config.empty());
        }
    }

    SecretServiceMpConfigSource(Config metaConfig, int ordinal) {
        this.prefix = metaConfig.get("prefix").asString().orElseThrow();
        if (DEFAULT_PREFIX.equals(prefix)) {
            defaultSecretServiceClient = SecretServiceClient.create(metaConfig);
        } else {
            this.secretServiceClient = SecretServiceClient.create(metaConfig);
        }
        this.ordinal = ordinal;
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
        if (!prop.hasPrefix() || prop.path().isBlank()) {
            return null;
        }

        try {
            return client().getSecret(prop.path())
                    .map(bytes -> new String(bytes, UTF_8))
                    .orElse(null);
        } catch (Exception e) {
            // config sources should not throw exceptions
            LOGGER.log(Logger.Level.WARNING, "Failed to obtain secret with the correct prefix. Property: " + propertyName, e);
            return null;
        }
    }

    @Override
    public String getName() {
        return "oci-secret-service";
    }

    SecretServiceClient client() {
        if (defaultSecretServiceClient != null) {
            return defaultSecretServiceClient;
        }

        if (secretServiceClient != null) {
            return secretServiceClient;
        }

        throw new IllegalStateException("No initialized client found");
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
        if (hasPrefix && prefix.length() != rawName.length()) {
            path = rawName.substring(prefix.length());
        } else {
            path = rawName;
        }
        return new Prop(profile, hasPrefix, path);
    }

    record Prop(Optional<String> profile, boolean hasPrefix, String path) {
    }
}
