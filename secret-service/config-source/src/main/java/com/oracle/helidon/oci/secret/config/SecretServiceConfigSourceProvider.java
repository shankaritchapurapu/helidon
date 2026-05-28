/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Set;

import io.helidon.config.AbstractConfigSource;
import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.ConfigSourceProvider;
import io.helidon.service.registry.Services;

/**
 * Helidon SE meta-config provider for the Secret Service config source.
 */
public final class SecretServiceConfigSourceProvider implements ConfigSourceProvider {
    private static final Set<String> SUPPORTED_TYPES = Set.of(SecretServiceConfigSource.TYPE);
    private static final String OCI_ENV_CONFIG_SOURCE = "oci-env";

    /**
     * Default constructor for service loading.
     */
    @Deprecated
    public SecretServiceConfigSourceProvider() {
    }

    @Deprecated
    @Override
    public AbstractConfigSource create(String type, Config metaConfig) {
        return SecretServiceConfigSource.builder()
                .ociEnvConfigSource(SecretServiceConfigSourceProvider::ociEnvConfigSource)
                .config(SecretServiceConfigSourceFactory.providerConfig(metaConfig))
                .build();
    }

    @Deprecated
    @Override
    public Set<String> supported() {
        return SUPPORTED_TYPES;
    }

    @Deprecated
    @Override
    public boolean supports(String type) {
        return SUPPORTED_TYPES.contains(type);
    }

    static java.util.Optional<ConfigSource> ociEnvConfigSource() {
        return Services.firstNamed(ConfigSource.class, OCI_ENV_CONFIG_SOURCE);
    }
}
