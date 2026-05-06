/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Set;

import io.helidon.common.Weight;
import io.helidon.config.AbstractConfigSource;
import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSourceProvider;

/**
 * Helidon SE meta-config provider for the Secret Service config source.
 */
@Weight(300D)
public final class SecretServiceConfigSourceProvider implements ConfigSourceProvider {
    private static final Set<String> SUPPORTED_TYPES = Set.of(SecretServiceConfigSource.TYPE);

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
                .config(metaConfig)
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
}
