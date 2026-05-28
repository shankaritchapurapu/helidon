/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.identity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.Map;

import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@ServerTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IdentityEndpointSplatAwareTest extends IdentityEndpointBase {
    private static final int SPLAT_REQUEST_PORT = findAvailablePort();

    static {
        // needs to run in a fresh Java VM
        System.setProperty("helidon.config.profile", "splat");
        System.setProperty("oci.identity.splat-aware.splat-request-port", String.valueOf(SPLAT_REQUEST_PORT));
    }

    public IdentityEndpointSplatAwareTest(WebServer webServer) {
        super(webServer);
    }

    @SetUpServer
    static void setUpServer(WebServerConfig.Builder builder) {
        builder.port(SPLAT_REQUEST_PORT);
    }

    @Test
    @Order(1)
    void testPing() throws Exception {
        testPing(Status.OK_200.code(), "pong");
    }

    @Test
    @Order(2)
    void testSplatOnceSuccess() throws Exception {
        testSplatOnceSuccess(Status.OK_200.code(), "Hello World");
    }

    @Test
    @Order(3)
    void testSplatTwiceSuccess() throws Exception {
        testSplatTwiceSuccess(Status.OK_200.code(), "Hello WorldHello World");
    }

    @Override
    Map<String, String> signRequest(URI uri,
                                    String httpMethod,
                                    Map<String, List<String>> headers,
                                    Object body) {
        return Map.of();
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
