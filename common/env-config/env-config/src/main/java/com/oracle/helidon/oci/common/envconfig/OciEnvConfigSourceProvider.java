/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.envconfig;

import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.ConfigSourceProvider;

/**
 * Helidon SE MetaConfig Provider for common Oci environment properties.
 */
public class OciEnvConfigSourceProvider implements ConfigSourceProvider {
    private static final Set<String> SUPPORTED_TYPES = Set.of("oci-env");

    /**
     * Checks if specified type is supported.
     */
    @Override
    public boolean supports(String type) {
        return this.supported().contains(type);
    }

    /**
     * Creates a new {@link ConfigSource}.
     */
    @Override
    public ConfigSource create(String type, Config metaConfig) {
        return new OciEnvConfigSource(metaConfig).get();
    }

    /**
     * Provides types that are supported, which in this case is oci-env.
     */
    @Override
    public Set<String> supported() {
        return SUPPORTED_TYPES;
    }
}
