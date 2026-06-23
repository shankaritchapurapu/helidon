/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class StreamingStoreEndpointTest {
    private final Http1Client client;

    StreamingStoreEndpointTest(Http1Client client) {
        this.client = client;
    }

    @Test
    void testStreamRequiresServiceBackend() {
        try (Http1ClientResponse response = client.get("/store/stream")
                .queryParam("cursor", "cursor-1")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.NOT_FOUND_404));
            assertThat(response.as(String.class), containsString("Kiev streams require"));
        }
    }

    @Test
    void testStreamRequiresCursorBeforeResolvingBackend() {
        try (Http1ClientResponse response = client.get("/store/stream")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.BAD_REQUEST_400));
            assertThat(response.as(String.class), containsString("cursor query parameter is required"));
        }
    }

    @Test
    void testStreamRejectsInvalidLimitBeforeResolvingBackend() {
        try (Http1ClientResponse response = client.get("/store/stream")
                .queryParam("limit", "abc")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.BAD_REQUEST_400));
        }
    }
}
