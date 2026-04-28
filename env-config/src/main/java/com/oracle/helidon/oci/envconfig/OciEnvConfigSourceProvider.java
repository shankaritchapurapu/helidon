/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.ConfigSourceProvider;

/**
 * Helidon SE config source provider for OCI environment information.
 */
public final class OciEnvConfigSourceProvider implements ConfigSourceProvider {
    static final String TYPE = "oci-env";

    @Override
    public boolean supports(String type) {
        return TYPE.equals(type);
    }

    @Override
    public ConfigSource create(String type, Config metaConfig) {
        return new OciEnvConfigSource(metaConfig);
    }

    @Override
    public Set<String> supported() {
        return Set.of(TYPE);
    }
}
