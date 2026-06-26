/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.secretservice;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RoutingTest
class Ssv2EndpointTest {
    private static final String SECRET_VALUE = "Helidon secret value";

    private final DirectClient client;

    Ssv2EndpointTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        Config config = Config.just(ConfigSources.create(Map.of("oci.ssv2/secret/my-app/secret-path/latest", SECRET_VALUE)));
        Ssv2Endpoint endpoint = new Ssv2Endpoint(config);

        router.get("/secret", (req, res) -> res.send(endpoint.secret()));
    }

    @Test
    void testReadsConfiguredSecret() {
        try (Http1ClientResponse response = client.get("/secret")
                .header(HeaderValues.ACCEPT_TEXT)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(SECRET_VALUE));
        }
    }
}
