/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.envconfig;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.service.registry.Service;

import com.oracle.pic.commons.util.Region;

@Service.Singleton
@Service.ExternalContracts(Region.class)
class PicRegionProvider implements Supplier<Optional<Region>> {

    private final LazyValue<Optional<Region>> region;

    @Service.Inject
    PicRegionProvider(Supplier<OciEnvConfigFactory> factory) {
        this.region = LazyValue.create(() -> factory.get().region());
    }

    @Override
    public Optional<Region> get() {
        return region.get();
    }
}
