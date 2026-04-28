/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.splat;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

/**
 * Minimal generated endpoint that demonstrates the SPLAT interception path.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@Http.Path("/splat")
@Service.Singleton
class SplatEndpoint {

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String get() {
        return "splat-ok";
    }
}
