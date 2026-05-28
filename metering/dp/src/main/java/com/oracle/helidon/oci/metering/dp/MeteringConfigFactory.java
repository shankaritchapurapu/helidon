/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory that creates data plane metering configuration from the Helidon config tree.
 *
 * @param config Helidon configuration root
 */
@Service.Singleton
record MeteringConfigFactory(Config config) implements Supplier<MeteringConfig> {

    @Override
    public MeteringConfig get() {
        return MeteringConfig.create(config.get("oci.metering"));
    }
}
