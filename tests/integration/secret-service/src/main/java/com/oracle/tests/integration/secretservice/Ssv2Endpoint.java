/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.secretservice;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.http.Http;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

@RestServer.Endpoint
@Http.Path("/secret")
@Service.Singleton
class Ssv2Endpoint {
    static final String SECRET_CONFIG_KEY = "oci.ssv2/secret/helidon/helidon-path/latest";

    private final Config config;

    @Service.Inject
    Ssv2Endpoint(Config config) {
        this.config = config;
    }

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String secret() {
        return config.get(SECRET_CONFIG_KEY)
                .asString()
                .orElseThrow(() -> new HttpException("SSv2 secret was not resolved: " + SECRET_CONFIG_KEY,
                                                      Status.NOT_FOUND_404));
    }
}
