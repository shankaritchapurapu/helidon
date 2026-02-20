/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.spi.OciRegion;
import io.helidon.service.registry.Service;

import com.oracle.bmc.Region;

@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT - 100)
class RegionProviderSdk implements OciRegion {
    private final LazyValue<Optional<Region>> region;

    RegionProviderSdk(Supplier<OciConfig> config) {
        this.region = LazyValue.create(() -> Optional.ofNullable(regionFromImds(config.get())));
    }

    /**
     * There is a 30 second timeout configured, so this has a relatively low weight.
     * We want a different way to get the region if available.
     */
    static Region regionFromImds(OciConfig ociConfig) {
        if (HelidonOci.imdsAvailable(ociConfig)) {
            return regionFromImdsDirect(ociConfig);
        }
        return null;
    }

    /**
     * Only called when we know imds is available.
     */
    static Region regionFromImdsDirect(OciConfig ociConfig) {
        Region.registerFromInstanceMetadataService();
        return Region.getRegionFromImds();
    }

    @Override
    public Optional<Region> region() {
        return region.get();
    }
}
