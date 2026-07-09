/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class UserAgentInfoTest {

    @Test
    void aggregatesProgressivelyForDefinedUserAgent() {
        UserAgentInfo userAgentInfo = UserAgentInfo.builder()
                .state(UserAgentInfo.State.DEFINED)
                .clientName("JavaSDK")
                .clientVersion("312")
                .osName("linux")
                .osVersion("510")
                .lang("java")
                .langVersion("1704")
                .build();

        assertThat(userAgentInfo.toMetricsString(), is("JavaSDK.312.linux.510.java.1704"));
        assertThat(userAgentInfo.toAggregatedMetricStrings(),
                   is(List.of("JavaSDK",
                              "JavaSDK.312",
                              "JavaSDK.312.linux",
                              "JavaSDK.312.linux.510",
                              "JavaSDK.312.linux.510.java",
                              "JavaSDK.312.linux.510.java.1704")));
    }

    @Test
    void unknownAndUndefinedAggregateAsSingleStateMetric() {
        assertThat(UserAgentInfo.UNKNOWN.toAggregatedMetricStrings(), is(List.of("UNKNOWN")));
        assertThat(UserAgentInfo.UNDEFINED.toAggregatedMetricStrings(), is(List.of("UNDEFINED")));
    }
}
