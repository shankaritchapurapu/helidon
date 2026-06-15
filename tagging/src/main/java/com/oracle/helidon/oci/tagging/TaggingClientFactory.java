/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tagging;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.pic.tagging.client.entities.TaggingClient;
import com.oracle.pic.tagging.client.entities.TaggingClientImpl;

/**
 * Factory that creates configured local tag slug {@link TaggingClient} instances.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class TaggingClientFactory implements Supplier<TaggingClient> {
    private final TaggingClientConfig config;

    @Service.Inject
    TaggingClientFactory(TaggingClientConfig config) {
        this.config = config;
    }

    @Override
    public TaggingClient get() {
        return TaggingClientImpl.builder()
                .emitMetrics(config.emitMetrics())
                .build();
    }
}
