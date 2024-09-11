/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.bmc.Region;

@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT + 1000)
@Service.ExternalContracts(Region.class)
class RegionTestProvider implements Supplier<Optional<Region>> {

    static Region REGION = Region.AP_MELBOURNE_1;

    @Override
    public Optional<Region> get() {
        return Optional.of(REGION);
    }
}