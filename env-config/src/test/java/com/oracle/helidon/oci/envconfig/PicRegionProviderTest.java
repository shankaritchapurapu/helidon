/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.helidon.config.Config;

import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

class PicRegionProviderTest {

    @Test
    void resolvesRegionLazilyAndCachesResult() {
        Region region = Region.fromPublicRegionName("us-ashburn-1");
        TestFactory factory = new TestFactory(() -> Optional.of(region));
        AtomicInteger factorySupplierCalls = new AtomicInteger();

        PicRegionProvider provider = new PicRegionProvider(() -> {
            factorySupplierCalls.incrementAndGet();
            return factory;
        });

        assertThat(factorySupplierCalls.get(), is(0));
        assertThat(factory.regionCalls(), is(0));

        assertThat(provider.get().orElseThrow(), sameInstance(region));
        assertThat(provider.get().orElseThrow(), sameInstance(region));
        assertThat(factorySupplierCalls.get(), is(1));
        assertThat(factory.regionCalls(), is(1));
    }

    @Test
    void returnsEmptyWhenRegionUnavailable() {
        PicRegionProvider provider = new PicRegionProvider(() -> new TestFactory(Optional::empty));

        assertThat(provider.get().isEmpty(), is(true));
    }

    private static class TestFactory extends OciEnvConfigFactory {
        private final Supplier<Optional<Region>> region;
        private final AtomicInteger regionCalls = new AtomicInteger();

        TestFactory(Supplier<Optional<Region>> region) {
            super(Config.empty());
            this.region = region;
        }

        @Override
        Optional<Region> region() {
            regionCalls.incrementAndGet();
            return region.get();
        }

        int regionCalls() {
            return regionCalls.get();
        }
    }
}
