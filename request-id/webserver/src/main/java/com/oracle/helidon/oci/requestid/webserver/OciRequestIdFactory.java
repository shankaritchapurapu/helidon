/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.requestid.webserver;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ServerRequest;

import com.oracle.helidon.oci.requestid.OciRequestId;

@Service.PerRequest
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class OciRequestIdFactory implements Supplier<OciRequestId> {

    private final ServerRequest request;

    @Service.Inject
    OciRequestIdFactory(ServerRequest request) {
        this.request = request;
    }

    @Override
    public OciRequestId get() {
        return request.context().get(OciRequestId.class).orElseThrow();
    }
}
