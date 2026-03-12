/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.function.Supplier;

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
class IdentityConfigFactory implements Supplier<IdentityConfig> {

    private final Config config;

    IdentityConfigFactory(Config config) {
        this.config = config;
    }

    /**
     * Creates a new {@link IdentityConfig} from the {@code oci.identity} configuration
     * section.
     *
     * @return a newly created {@link IdentityConfig}
     */
    @Override
    public IdentityConfig get() {
        return IdentityConfig.create(config.get("oci.identity"));
    }
}
