/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OciHttpEndpointMetricsContextTest {

    @Test
    void resourceElapsedNanosRequiresCapturedTiming() {
        OciHttpEndpointMetricsContext context = context();

        assertThat(context.hasResourceTiming(), is(false));
        assertThrows(IllegalStateException.class, context::resourceElapsedNanos);
    }

    @Test
    void resourceElapsedNanosReturnsCapturedDuration() {
        OciHttpEndpointMetricsContext context = context();

        context.markResourceStart();
        context.markResourceEnd(false);

        assertThat(context.hasResourceTiming(), is(true));
        assertThat(context.resourceElapsedNanos(), greaterThanOrEqualTo(0L));
    }

    private static OciHttpEndpointMetricsContext context() {
        return new OciHttpEndpointMetricsContext("StoreEndpoint.get",
                                                 List.of(),
                                                 "com.example.store.StoreEndpoint");
    }
}
