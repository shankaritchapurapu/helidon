/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.util.List;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.mp.Prioritized;
import io.helidon.config.mp.spi.MpMetaConfigProvider;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * OCI SSv2 MP config provider.
 */
public class SecretServiceMpMetaConfigProvider implements MpMetaConfigProvider, Prioritized {
    @Override
    public Set<String> supportedTypes() {
        return Set.of("oci-secret-service");
    }

    @Override
    public List<? extends ConfigSource> create(String type, Config metaConfig, String profile) {
        return List.of(new SecretServiceMpConfigSource(metaConfig, 93));
    }

    @Override
    public int priority() {
        return 93;
    }
}
