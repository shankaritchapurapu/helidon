/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * A factory to create an instance of {@link LimitsConfig}.
 *
 * @param config Helidon configuration root
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
record LimitsConfigFactory(Config config) implements Supplier<LimitsConfig> {

    /**
     * Get a new instance of {@link LimitsConfig}.
     *
     * @return a new instance of {@link LimitsConfig}
     */
    @Override
    public LimitsConfig get() {
        return LimitsConfig.create(config.get("oci.limits"));
    }
}
