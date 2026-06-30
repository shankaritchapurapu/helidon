/*
 * Copyright (c) 2023, 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.requestid;

import java.util.List;

import io.helidon.logging.common.LogConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class OciRequestIdImplTest {
    @BeforeAll
    static void setup() {
        LogConfig.configureRuntime();
    }

    @Test
    void testValidNotStripped() {
        String valid = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        String actual = OciRequestIdImpl.stripNotAllowedCharacters(valid);

        assertThat(actual, is(valid));
    }

    @Test
    void testInvalidStripped() {
        char[] allChars = new char[255];
        for (int i = 0; i < allChars.length; i++) {
            allChars[i] = (char) i;
        }
        String valid = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
        String toTest = new String(allChars);
        String actual = OciRequestIdImpl.stripNotAllowedCharacters(toTest);

        assertThat(actual, is(valid));
    }

    @Test
    void testNonAscii() {
        String valid = "id";
        String toTest = "\u010D\u0159id\u017E";
        String actual = OciRequestIdImpl.stripNotAllowedCharacters(toTest);

        assertThat(actual, is(valid));
    }

    @Test
    void testUpstreamContainsSpanId() {
        String id = "1/2/3";
        OciRequestId ociRequestId = OciRequestIdImpl.parseUpstreamRequest(id);
        assertThat(ociRequestId.customerId(), is("1"));
        assertThat(ociRequestId.traceId(), is("2"));
        assertThat(ociRequestId.spanId(), not("3"));
    }

    @Test
    void testUpstreamContainsTooManyParts() {
        String id = "1/2/3/4";
        OciRequestId ociRequestId = OciRequestIdImpl.parseUpstreamRequest(id);
        assertThat(ociRequestId.customerId(), is("1"));
        assertThat(ociRequestId.traceId(), is("2"));
        assertThat(ociRequestId.spanId(), not("3"));
    }

    @Test
    void testMalformedUpstreamValuesDoNotThrow() {
        for (String id : List.of("", "/", "//", "///", "////", "customer/", "customer/!", "tenant/!!!/ignored")) {
            OciRequestId ociRequestId = OciRequestIdImpl.parseUpstreamRequest(id);
            assertThat(ociRequestId.traceId(), not(""));
            assertThat(ociRequestId.spanId(), not(""));
        }
    }

    @Test
    void testMalformedUpstreamValuePreservesCustomerId() {
        OciRequestId ociRequestId = OciRequestIdImpl.parseUpstreamRequest("customer/");

        assertThat(ociRequestId.customerId(), is("customer"));
        assertThat(ociRequestId.traceId(), not(""));
        assertThat(ociRequestId.spanId(), not(""));
    }
}
