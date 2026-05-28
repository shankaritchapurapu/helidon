/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.config.ConfigException;

import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionSupportTest {
    @Test
    void resolvesConfiguredPublicRegionName() {
        assertThat(region(Optional.of("us-phoenix-1")),
                   is(Region.fromPublicRegionName("us-phoenix-1")));
    }

    @Test
    void resolvesConfiguredInternalRegionName() {
        assertThat(region(Optional.of("r2")), is(Region.fromInternalName("r2")));
    }

    @Test
    void resolvesConfiguredAirportCode() {
        assertThat(region(Optional.of("PHX")), is(Region.fromAirportCode("PHX")));
    }

    @Test
    void resolvesAbsentConfiguredRegionFromSupplier() {
        Region suppliedRegion = Region.fromPublicRegionName("us-ashburn-1");

        assertThat(region(Optional.empty(), suppliedRegion), is(suppliedRegion));
    }

    @Test
    void rejectsInvalidConfiguredRegion() {
        ConfigException exception = assertThrows(ConfigException.class,
                                                () -> RegionSupport.resolve(Optional.of("not-a-region"), () -> {
                                                    throw new AssertionError();
                                                }));

        assertThat(exception.getMessage(), containsString("Invalid OCI region: not-a-region"));
    }

    private static Region region(Optional<String> configuredRegion) {
        return region(configuredRegion, Region.fromPublicRegionName("us-ashburn-1"));
    }

    private static Region region(Optional<String> configuredRegion, Region suppliedRegion) {
        return RegionSupport.resolve(configuredRegion, () -> suppliedRegion);
    }
}
