/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * A factory to create an instance of {@link com.oracle.helidon.oci.identity.IdentityConfig}.
 */
@Service.Singleton
record IdentityConfigFactory(Config config) implements Supplier<IdentityConfig> {

    /**
     * Get a new instance of {@link com.oracle.helidon.oci.identity.IdentityConfig}.
     *
     * @return a new instance of {@link com.oracle.helidon.oci.identity.IdentityConfig}
     */
    @Override
    public IdentityConfig get() {
        return IdentityConfig.create(config.get("oci.identity"));
    }
}
