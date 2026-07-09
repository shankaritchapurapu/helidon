/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class UserAgentParserCollatorTest {

    @Test
    void parsesJavaSdkUserAgent() {
        UserAgentParserCollator collator = UserAgentParserCollator.createDefault();

        UserAgentInfo userAgentInfo =
                collator.parse("Oracle-JavaSDK/3.1.2 (Mac OS X/13.0; Java/17.0.4; vendor)");

        assertThat(userAgentInfo.toMetricsString(), is("JavaSDK.312.macosx.130.java.1704"));
    }

    @Test
    void parsesBrowserUserAgentAsClientNameOnly() {
        UserAgentParserCollator collator = UserAgentParserCollator.createDefault();

        UserAgentInfo userAgentInfo = collator.parse("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");

        assertThat(userAgentInfo.toMetricsString(), is("BrowserClient.UNKNOWN.UNKNOWN.UNKNOWN.UNKNOWN.UNKNOWN"));
    }

    @Test
    void returnsUndefinedForBlankUserAgent() {
        assertThat(UserAgentParserCollator.createDefault().parse(" ").toMetricsString(), is("UNDEFINED"));
    }

    @Test
    void returnsUnknownForUnmatchedUserAgent() {
        assertThat(UserAgentParserCollator.createDefault().parse("not-a-known-client/1.0").toMetricsString(),
                   is("UNKNOWN"));
    }
}
