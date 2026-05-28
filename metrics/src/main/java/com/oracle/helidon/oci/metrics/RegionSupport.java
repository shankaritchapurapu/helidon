/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.config.ConfigException;

import com.oracle.pic.commons.util.Region;

final class RegionSupport {
    private RegionSupport() {
    }

    static Region resolve(Optional<String> configuredRegion, Supplier<Region> regionSupplier) {
        return configuredRegion
                .map(RegionSupport::configuredRegion)
                .orElseGet(regionSupplier);
    }

    private static Region configuredRegion(String configuredRegion) {
        return tryResolve(() -> Region.fromPublicRegionName(configuredRegion))
                .or(() -> tryResolve(() -> Region.fromInternalName(configuredRegion)))
                .or(() -> tryResolve(() -> Region.fromAirportCode(configuredRegion)))
                .orElseThrow(() -> new ConfigException("Invalid OCI region: " + configuredRegion));
    }

    private static Optional<Region> tryResolve(Supplier<Region> regionSupplier) {
        try {
            return Optional.of(regionSupplier.get());
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return Optional.empty();
        }
    }
}
