/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.Map;
import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.bmc.Region;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class OciEnvRegionProviderTest {

    @Test
    void fallsBackToSdkRegistrationWhenSdkLookupDoesNotKnowRegion() {
        Region region = OciEnvRegionImpl.toSdkRegion(com.oracle.pic.commons.util.Region.SOL_MARS_1,
                                                         ignored -> {
                                                             throw new IllegalArgumentException("forced for test");
                                                         })
                .orElseThrow();

        assertThat(region.getRegionId(), is("sol-mars-1"));
        assertThat(region.getRealm().getSecondLevelDomain(),
                   is(com.oracle.pic.commons.util.Region.SOL_MARS_1.getRealm()
                              .getPublicDomainName()
                              .orElseThrow()));
    }

    @Test
    void returnsEmptyWhenEnvironmentRegionUnavailable() {
        OciEnvRegionImpl provider = new OciEnvRegionImpl(OciEnvRegionProviderTest::factoryWithoutRegion);

        assertThat(provider.region().isEmpty(), is(true));
    }

    @Test
    void registersSyntheticSdkRegionWhenCommonsRealmHasNoSdkDomainMapping() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.enabled", "false"),
                entry("location-override-dev", "true"))));

        OciEnvRegionImpl provider = new OciEnvRegionImpl(() -> new OciEnvConfigFactory(config));

        Region region = provider.region().orElseThrow();

        assertThat(region.getRegionId(), is("dev"));
        assertThat(region.getRealm().getRealmId(), is(com.oracle.pic.commons.util.Region.DEV.getRealm().getName()));
        assertThat(region.getRealm().getSecondLevelDomain(), is("oraclecloud.invalid"));
    }

    private static OciEnvConfigFactory factoryWithoutRegion() {
        return new OciEnvConfigFactory(Config.just(ConfigSources.create(Map.of()))) {
            @Override
            Optional<com.oracle.pic.commons.util.Region> region() {
                return Optional.empty();
            }
        };
    }
}
