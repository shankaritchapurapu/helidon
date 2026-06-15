/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory for creating {@link IdentityConfig} instances from application configuration.
 * <p>
 * This factory reads the {@code oci.identity} subtree from the provided
 * {@link Config} instance and uses it to construct a fully initialized
 * {@link IdentityConfig}. It is registered as a {@link Service.Singleton},
 * so the same configuration source is reused for all created instances.
 * </p>
 *
 * <p>
 * Typical usage is to inject this factory wherever an {@link IdentityConfig}
 * is needed and obtain the configuration via {@link #get()}.
 * </p>
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class IdentityConfigFactory implements Supplier<IdentityConfig> {

    private final Config config;

    IdentityConfigFactory(Config config) {
        this.config = config;
    }

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
