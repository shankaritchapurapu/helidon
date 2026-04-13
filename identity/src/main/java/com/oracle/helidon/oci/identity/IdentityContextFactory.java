/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ServerRequest;

@Service.PerRequest
class IdentityContextFactory implements Supplier<IdentityContext> {

    private final ServerRequest request;

    @Service.Inject
    IdentityContextFactory(ServerRequest request) {
        this.request = request;
    }

    @Override
    public IdentityContext get() {
        return request.context().get(IdentityContext.class).orElseThrow();
    }
}
