/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.requestid.microprofile;

import io.helidon.common.context.Contexts;

import com.oracle.helidon.oci.common.requestid.OciRequestId;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Forwards the opc-request-id header value on every REST client request. Only
 * 2 of the 3 parts are forwarded: the so-called downstream request ID.
 */
@Priority(Priorities.AUTHENTICATION - 100)
class RequestIdClientFilter implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(OciRequestId.OCI_REQUEST_ID);

        // if empty, try to get the current one from context
        if (requestId == null || requestId.isEmpty()) {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();

            // find request id in our context and use it
            Contexts.context()
                    .flatMap(c -> c.get(OciRequestId.class))
                    .ifPresent(id -> {
                        String clientRequestId = id.downstreamHeaderValue();
                        headers.putSingle(OciRequestId.OCI_REQUEST_ID, clientRequestId);
                    });
        }
    }
}
