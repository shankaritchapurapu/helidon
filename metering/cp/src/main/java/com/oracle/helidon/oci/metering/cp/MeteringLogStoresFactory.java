/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.pic.bling.emit.MeteringLogStores;

/**
 * Factory that exposes metering-agent metering log stores for control plane metering.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class MeteringLogStoresFactory implements Supplier<MeteringLogStores> {
    private final LazyValue<MeteringLogStores> logStores;

    @Service.Inject
    MeteringLogStoresFactory(MeteringRuntime runtime) {
        this.logStores = LazyValue.create(runtime::meteringLogStores);
    }

    @Override
    public MeteringLogStores get() {
        return logStores.get();
    }
}
