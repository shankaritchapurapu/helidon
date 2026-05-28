/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OciEnvLocationDefaultsTest {

    @Test
    void explicitRegionWinsOverDefaultRegion() {
        AtomicBoolean called = new AtomicBoolean();
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(Config.empty(), recordingDefaultRegion(called));

        Region region = defaults.resolveRegion(Optional.of("us-phoenix-1"), "missing");

        assertThat(region, is(Region.fromPublicRegionName("us-phoenix-1")));
        assertThat(called.get(), is(false));
    }

    @Test
    void usesDefaultRegionWhenExplicitRegionIsMissing() {
        AtomicBoolean called = new AtomicBoolean();
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(Config.empty(), recordingDefaultRegion(called));

        Region region = defaults.resolveRegion(Optional.empty(), "missing");

        assertThat(region, is(Region.fromPublicRegionName("us-ashburn-1")));
        assertThat(called.get(), is(true));
    }

    @Test
    void rejectsMissingExplicitAndDefaultRegion() {
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(Config.empty(), Optional::empty);

        assertThrows(IllegalStateException.class,
                     () -> defaults.resolveRegion(Optional.empty(), "missing"));
    }

    @Test
    void explicitAvailabilityDomainWinsOverOciEnvValue() {
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(ociEnvLocationConfig(), Optional::empty);

        assertThat(defaults.availabilityDomain(Optional.of("iad-ad-2")).orElseThrow(), is("iad-ad-2"));
    }

    @Test
    void usesOciEnvAvailabilityDomainWhenExplicitValueIsMissing() {
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(ociEnvLocationConfig(), Optional::empty);

        assertThat(defaults.availabilityDomain(Optional.empty()).orElseThrow(), is("iad-ad-1"));
    }

    @Test
    void rejectsMissingExplicitAndOciEnvAvailabilityDomain() {
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(Config.empty(), Optional::empty);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                       () -> defaults.requireAvailabilityDomain(Optional.empty(),
                                                                                                "missing ad"));

        assertThat(exception.getMessage(), is("missing ad"));
    }

    @Test
    void providesOciEnvFaultDomain() {
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(ociEnvLocationConfig(), Optional::empty);

        assertThat(defaults.faultDomain().orElseThrow(), is("2"));
    }

    @Test
    void derivesAuthenticationEndpointFromRegionRealmDomain() {
        Region region = Region.SOL_MARS_1;
        OciEnvLocationDefaults defaults = new OciEnvLocationDefaults(Config.empty(), Optional::empty);

        URI uri = defaults.authServiceUri(region);

        assertThat(uri,
                   is(URI.create("https://auth." + region.getPublicRegionName()
                                         + "." + region.getRealm().getPublicDomainName().orElseThrow())));
    }

    private static Supplier<Optional<Region>> recordingDefaultRegion(AtomicBoolean called) {
        return () -> {
            called.set(true);
            return Optional.of(Region.fromPublicRegionName("us-ashburn-1"));
        };
    }

    private static Config ociEnvLocationConfig() {
        return Config.just(ConfigSources.create(Map.of(
                "oci.env.availability-domain", "iad-ad-1",
                "oci.env.fault-domain", "2")));
    }
}
