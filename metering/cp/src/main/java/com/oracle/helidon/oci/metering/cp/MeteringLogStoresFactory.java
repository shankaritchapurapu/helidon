/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.service.registry.Service;

import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.kiev.mapping.MappedDataStore;

/**
 * Factory that exposes metering-agent metering log stores for agent-backed control plane metering.
 */
@Service.Singleton
class MeteringLogStoresFactory implements Supplier<MeteringLogStores> {
    private final LazyValue<MeteringLogStores> logStores;

    @Service.Inject
    MeteringLogStoresFactory(MappedDataStore mappedDataStore,
                             com.oracle.pic.bling.emit.config.MeteringAgentConfig nativeConfig) {
        this.logStores = LazyValue.create(() -> new MeteringLogStores(mappedDataStore,
                                                                       nativeConfig.getBucketConfigs(),
                                                                       nativeConfig.getBucketV3Configs(),
                                                                       nativeConfig.isDuplicateV2WritesDisabled()));
    }

    @Override
    public MeteringLogStores get() {
        return logStores.get();
    }
}
