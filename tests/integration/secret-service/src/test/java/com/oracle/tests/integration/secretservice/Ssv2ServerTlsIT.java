/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.secretservice;

import java.net.URI;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import io.helidon.webserver.testing.junit5.SetUpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class Ssv2ServerTlsIT {
    private static final String SERVER_TLS_ESTABLISHED = "Server TLS established";
    private static final Config CONFIG = Config.just(() -> ConfigSources.classpath("ssv2-server-tls.yaml").build());

    private final URI serverUri;
    private final Http1Client client;

    Ssv2ServerTlsIT(URI serverUri) {
        this.serverUri = serverUri;
        this.client = Http1Client.builder()
                .config(CONFIG.get("client"))
                .baseUri(serverUri)
                .keepAlive(false)
                .build();
    }

    @SetUpServer
    static void setUpServer(WebServerConfig.Builder server) {
        server.config(CONFIG.get("server"));
    }

    @SetUpRoute
    static void setUpRoute(HttpRouting.Builder routing) {
        routing.get("/tls", (request, response) -> response.send(SERVER_TLS_ESTABLISHED));
    }

    @AfterEach
    void closeClient() {
        client.closeResource();
    }

    @Test
    void establishesServerTlsWithPkiMaterialFromSsv2() {
        assertThat(serverUri.getScheme(), is("https"));
        try (Http1ClientResponse response = client.get("/tls").request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(SERVER_TLS_ESTABLISHED));
        }
    }
}
