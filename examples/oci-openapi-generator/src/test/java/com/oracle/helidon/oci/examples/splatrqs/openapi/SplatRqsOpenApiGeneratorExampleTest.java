/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.examples.splatrqs.openapi;

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
class SplatRqsOpenApiGeneratorExampleTest {

    private static final String CREATE_ROBOT_BODY = """
            {
              "compartmentId": "ocid1.compartment.oc1..example",
              "displayName": "rqs-example-robot"
            }
            """;

    private final Http1Client client;

    SplatRqsOpenApiGeneratorExampleTest(Http1Client client) {
        this.client = client;
    }

    @Test
    void generatedCreateRobotRouteIsRegistered() {
        try (Http1ClientResponse response = client.post("/robots")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(CREATE_ROBOT_BODY)) {
            assertGeneratedStubWasReached(response);
        }
    }

    @Test
    void generatedDeleteRobotRouteIsRegistered() {
        try (Http1ClientResponse response = client.delete("/robots/robot-1")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertGeneratedStubWasReached(response);
        }
    }

    @Test
    void generatedOpenApiContainsSplatRqsMetadata() {
        try (Http1ClientResponse response = client.get("/openapi")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));

            String body = response.as(String.class);
            assertThat(body, containsString("openapi: 3."));
            assertThat(body, containsString("x-obmcs-splat"));
            assertThat(body, containsString("searchMetadata"));
            assertThat(body, containsString("resourceType: helidon-example-robot"));
        }
    }

    private static void assertGeneratedStubWasReached(Http1ClientResponse response) {
        // Generated endpoints are stubs until the service owner adds business logic.
        // A 500 here means the generated route and parameter/entity binding were reached.
        assertThat(response.status(), is(Status.INTERNAL_SERVER_ERROR_500));
    }
}
