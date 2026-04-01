/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory for creating {@link KievConfig} from application configuration.
 */
@Service.Singleton
class KievConfigFactory implements Supplier<KievConfig> {

    private final Config config;

    KievConfigFactory(Config config) {
        this.config = config;
    }

    @Override
    public KievConfig get() {
        return KievConfig.create(config.get("oci.kiev"));
    }
}
