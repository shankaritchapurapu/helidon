/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.requestid;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.helidon.oci.requestid.OciRequestId;

/**
 * Small endpoint that exposes the per-request {@link OciRequestId}.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@Http.Path("/request-id")
@Service.Singleton
class RequestIdEndpoint {

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String get(OciRequestId requestId) {
        return "upstream=" + requestId.upstreamHeaderValue() + "\n"
                + "customer=" + requestId.customerId() + "\n"
                + "trace=" + requestId.traceId() + "\n"
                + "span=" + requestId.spanId();
    }
}
