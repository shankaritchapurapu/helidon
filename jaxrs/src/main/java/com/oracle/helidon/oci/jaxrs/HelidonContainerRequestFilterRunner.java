/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * Executes JAX-RS request filters against Helidon request/response objects.
 */
public final class HelidonContainerRequestFilterRunner {

    private HelidonContainerRequestFilterRunner() {
    }

    /**
     * Execute a JAX-RS request filter against the current Helidon request.
     *
     * @param filter the filter to execute
     * @param request the current server request
     * @param response the current server response
     * @param resourceInfo the matched resource info
     * @return the populated request context if the filter allows processing to continue;
     *         empty when the filter has already sent a response
     */
    public static Optional<HelidonContainerRequestContext> run(ContainerRequestFilter filter,
                                                               ServerRequest request,
                                                               ServerResponse response,
                                                               ResourceInfo resourceInfo) throws Exception {
        HelidonContainerRequestContext context = new HelidonContainerRequestContext(request, resourceInfo);
        HelidonContextInjector.inject(filter, context);
        HelidonContextInjector.postConstruct(filter);

        // Buffering a bodyless request delegates to an empty entity implementation,
        // whose default buffer() method throws UnsupportedOperationException.
        if (context.hasEntity()) {
            request.content().buffer();
        }

        try {
            filter.filter(context);
        } catch (WebApplicationException e) {
            sendResponse(response, e.getResponse());
            return Optional.empty();
        }

        if (context.isAborted()) {
            String msg = context.getAbortMessage();
            response.status(context.getAbortStatus()).send(msg != null ? msg : "");
            return Optional.empty();
        }

        return Optional.of(context);
    }

    private static void sendResponse(ServerResponse response, Response jaxRsResponse) {
        if (jaxRsResponse.hasEntity()) {
            response.status(jaxRsResponse.getStatus()).send(jaxRsResponse.getEntity());
        } else {
            response.status(jaxRsResponse.getStatus()).send();
        }
    }
}
