/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.secretservice;

import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class Ssv2EndpointIT {
    private static final String EXPECTED_SECRET = "Helidon secret value";

    private final Http1Client client;

    Ssv2EndpointIT(Http1Client client) {
        this.client = client;
    }

    @Test
    void readsSecretThroughDeclarativeEndpoint() {
        try (Http1ClientResponse response = client.get("/secret")
                .header(HeaderValues.ACCEPT_TEXT)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(EXPECTED_SECRET));
        }
    }
}
