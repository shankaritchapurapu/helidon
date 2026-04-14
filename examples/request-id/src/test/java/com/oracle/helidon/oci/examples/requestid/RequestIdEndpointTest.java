/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.requestid;

import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpFeatures;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import com.oracle.helidon.oci.requestid.OciRequestId;
import com.oracle.helidon.oci.requestid.webserver.RequestIdServerFeature;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

@RoutingTest
class RequestIdEndpointTest {
    private final DirectClient client;

    RequestIdEndpointTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        router.get("/request-id", (req, res) -> {
            RequestIdEndpoint endpoint = new RequestIdEndpoint();
            res.send(endpoint.get(req.context().get(OciRequestId.class).orElseThrow()));
        });
    }

    @SetUpFeatures
    static List<ServerFeature> features() {
        return List.of(RequestIdServerFeature.create());
    }

    @Test
    void testInjectedRequestIdMatchesResponseHeader() {
        try (Http1ClientResponse response = client.get("/request-id")
                .header(HeaderValues.ACCEPT_TEXT)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));

            String headerValue = response.headers().get(HeaderNames.create("opc-request-id")).getString();
            String body = response.as(String.class);

            assertThat(body, containsString("upstream=" + headerValue));
            assertThat(body, containsString("trace="));
            assertThat(body, containsString("span="));
        }
    }
}
