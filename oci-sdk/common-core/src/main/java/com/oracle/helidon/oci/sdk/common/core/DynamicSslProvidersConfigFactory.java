/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory for reusable dynamic SSL context provider configuration.
 *
 * @param config Helidon configuration root
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
record DynamicSslProvidersConfigFactory(Config config) implements Supplier<DynamicSslProvidersConfig> {

    @Override
    public DynamicSslProvidersConfig get() {
        return DynamicSslProvidersConfig.create(config.get("oci"));
    }
}
