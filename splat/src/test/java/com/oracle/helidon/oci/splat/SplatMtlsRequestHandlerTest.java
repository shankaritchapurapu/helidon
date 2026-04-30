/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import javax.ws.rs.container.ContainerRequestFilter;

import org.junit.jupiter.api.Test;

import com.oracle.pic.commons.util.Region;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplatMtlsRequestHandlerTest {

    @Test
    void shouldTranslateHelidonConfigToUpstreamConfig() {
        SplatMtlsConfig config = SplatMtlsConfig.builder()
                .region("us-ashburn-1")
                .skipAuthzValidationCheck(true)
                .rejectXRegionCalls(true)
                .buildPrototype();
        SplatMtlsRequestHandler handler = new SplatMtlsRequestHandler(config,
                                                                      SplatMtlsRequestHandlerTest::failRegionLookup);

        var upstreamConfig = handler.upstreamConfig();

        assertThat(handler.resolveRegion(), is(Region.fromPublicRegionName("us-ashburn-1")));
        assertThat(upstreamConfig.isSkipAuthzValidationCheck(), is(true));
        assertThat(upstreamConfig.isRejectXRegionCalls(), is(true));
    }

    @Test
    void shouldUseDefaultRegionWhenConfigDoesNotSpecifyOne() {
        SplatMtlsRequestHandler handler = new SplatMtlsRequestHandler(
                SplatMtlsConfig.builder().buildPrototype(),
                () -> Region.fromPublicRegionName("us-ashburn-1"));

        assertThat(handler.resolveRegion(), is(Region.fromPublicRegionName("us-ashburn-1")));
    }

    @Test
    void shouldNotCreateFilterWhenDisabled() {
        SplatMtlsRequestHandler handler = new SplatMtlsRequestHandler(
                SplatMtlsConfig.builder().enabled(false).buildPrototype(),
                SplatMtlsRequestHandlerTest::failRegionLookup);

        assertTrue(handler.createFilter().isEmpty());
    }

    @Test
    void shouldCreateJaxRsFilterWhenEnabled() {
        SplatMtlsRequestHandler handler = new SplatMtlsRequestHandler(
                SplatMtlsConfig.builder().region("us-ashburn-1").buildPrototype(),
                SplatMtlsRequestHandlerTest::failRegionLookup);

        assertTrue(handler.createFilter().filter(ContainerRequestFilter.class::isInstance).isPresent());
    }

    private static Region failRegionLookup() {
        throw new AssertionError("Default region should not be resolved");
    }
}
