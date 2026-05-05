/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class OciUnitConverterTest {

    @Test
    void normalizeDurationPreservesFractionalTargetUnits() {
        assertThat(OciUnitConverter.normalizeDuration(500, TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS), is(0.5D));
        assertThat(OciUnitConverter.normalizeDuration(1_500, TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS), is(1.5D));
    }

    @Test
    void normalizeDurationConvertsWholeTargetUnits() {
        assertThat(OciUnitConverter.normalizeDuration(2, TimeUnit.SECONDS, TimeUnit.MILLISECONDS), is(2_000D));
    }
}
