/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.requestid.webserver;

import io.helidon.common.Weight;
import io.helidon.common.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.spi.ServerFeature;

/**
 * Server feature that adds support for Request ID to Helidon WebServer.
 */
// after Context, before AccessLog
@Weight(1050)
public class RequestIdServerFeature implements ServerFeature {
    static final String TYPE = "oci-request-id";

    private final String name;

    private RequestIdServerFeature(String name) {
        this.name = name;
    }

    /**
     * Create a new feature to register with WebServer, when not using automatic feature discovery.
     *
     * @return a new server feature that adds OCI request id handling
     */
    public static RequestIdServerFeature create() {
        return create(Config.empty(), TYPE);
    }

    static RequestIdServerFeature create(Config config, String name) {
        return new RequestIdServerFeature(name);
    }

    @Override
    public void setup(ServerFeatureContext serverFeatureContext) {
        serverFeatureContext.socket(WebServer.DEFAULT_SOCKET_NAME)
                .httpRouting()
                .addFilter(new RequestIdServerFilter());
        for (String socket : serverFeatureContext.sockets()) {
            serverFeatureContext.socket(socket)
                    .httpRouting()
                    .addFilter(new RequestIdServerFilter());
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
