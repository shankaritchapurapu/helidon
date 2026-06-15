/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ServerRequest;

@Service.PerRequest
@Weight(Weighted.DEFAULT_WEIGHT - 30)
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
