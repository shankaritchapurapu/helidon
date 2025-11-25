/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.envconfig.microprofile;

import java.util.List;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.mp.Prioritized;
import io.helidon.config.mp.spi.MpMetaConfigProvider;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Helidon MP MetaConfig Provider for common Oci environment properties.
 */
public class OciEnvMpMetaConfigSourceProvider implements MpMetaConfigProvider, Prioritized {
    private static final Set<String> SUPPORTED_TYPES = Set.of("oci-env");
    private static final int DEFAULT_ORDINAL = 93;

    /**
     * Ensures that <strong>oci-env</strong> type is supported.
     */
    @Override
    public Set<String> supportedTypes() {
         return SUPPORTED_TYPES;
    }

    /**
     * Returns an immutable {@link java.util.List} that contains a {@link org.eclipse.microprofile.config.spi.ConfigSource} implementation backed by properties
     * retrieved from {@link com.oracle.pic.commons.configuration.EnvironmentConfig}.
     */
    @Override
    public List<? extends ConfigSource> create(String type, Config metaConfig, String profile) {
        return List.of(new OciEnvMpConfigSource(metaConfig, DEFAULT_ORDINAL));
    }

    /**
     * Sets priority for this Config Source Provider. A value of 93 is generally higher than most Config Source Providers.
     */
    @Override
    public int priority() {
        return DEFAULT_ORDINAL;
    }
}
