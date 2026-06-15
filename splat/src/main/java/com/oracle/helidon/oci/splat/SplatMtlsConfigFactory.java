/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * A factory to create an instance of {@link SplatMtlsConfig}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
record SplatMtlsConfigFactory(Config config) implements Supplier<SplatMtlsConfig> {

    /**
     * Get a new instance of {@link SplatMtlsConfig}.
     *
     * @return a new instance of {@link SplatMtlsConfig}
     */
    @Override
    public SplatMtlsConfig get() {
        return SplatMtlsConfig.create(config.get("oci.splat"));
    }
}
