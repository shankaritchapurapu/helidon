/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.envconfig.microprofile;

import java.util.List;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.mp.MpConfigSources;
import io.helidon.config.mp.Prioritized;
import io.helidon.config.mp.spi.MpMetaConfigProvider;

import com.oracle.helidon.oci.common.envconfig.OciEnvConfigSourceProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Helidon MP MetaConfig Provider for common Oci environment properties.
 */
public class OciEnvMpMetaConfigSourceProvider implements MpMetaConfigProvider, Prioritized {
    private final OciEnvConfigSourceProvider ociEnvConfigSourceProvider;

    /**
     * Creates a new {@link OciEnvMpMetaConfigSourceProvider}.
     *
     * @deprecated For use by the Helidon Config subsystem only.
     */
    @Deprecated // For java.util.ServiceLoader use only.
    public OciEnvMpMetaConfigSourceProvider() {
        super();
        this.ociEnvConfigSourceProvider = new OciEnvConfigSourceProvider();
    }

    /**
     * Returns the return value of an invocation of {@link OciEnvConfigSourceProvider#supported()}.
     */
    @Override
    public Set<String> supportedTypes() {
        return this.ociEnvConfigSourceProvider.supported();
    }

    /**
     * Returns an immutable {@link List} whose sole element is a {@link ConfigSource} implementation backed by properties
     * retrieved from {@link com.oracle.pic.commons.configuration.EnvironmentConfig}.
     */
    @Override
    public List<? extends ConfigSource> create(String type, Config metaConfig, String profile) {
        return List.of(MpConfigSources.create(this.ociEnvConfigSourceProvider.create(type, metaConfig)));
    }

    /**
     * Sets priority for this Config Source Provider. A value of 90 is generally higher than most Config Source Providers.
     */
    @Override
    public int priority() {
        return 90;
    }
}
