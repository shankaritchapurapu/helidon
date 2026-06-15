/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tagging;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory for {@link TaggingClientConfig}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class TaggingClientConfigFactory implements Supplier<TaggingClientConfig> {
    private final Config config;

    @Service.Inject
    TaggingClientConfigFactory(Config config) {
        this.config = config;
    }

    @Override
    public TaggingClientConfig get() {
        return TaggingClientConfig.create(config.get("oci.tagging"));
    }
}
