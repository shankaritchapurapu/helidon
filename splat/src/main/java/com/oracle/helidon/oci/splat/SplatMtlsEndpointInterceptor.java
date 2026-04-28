/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.HttpEntryPoint;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.oracle.helidon.oci.jaxrs.HelidonResourceInfo;

/**
 * Executes upstream SPLAT validation for generated Helidon {@code @RestServer.Endpoint} handlers.
 */
@Service.Singleton
final class SplatMtlsEndpointInterceptor implements HttpEntryPoint.Interceptor {
    private final SplatMtlsRequestHandler requestHandler;

    SplatMtlsEndpointInterceptor(SplatMtlsRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    static SplatMtlsEndpointInterceptor createForTesting(SplatMtlsRequestHandler requestHandler) {
        return new SplatMtlsEndpointInterceptor(requestHandler);
    }

    @Override
    public void proceed(InterceptionContext interceptionContext,
                        HttpEntryPoint.Interceptor.Chain chain,
                        ServerRequest request,
                        ServerResponse response) throws Exception {
        boolean allowed = requestHandler.shouldAllow(
                request,
                response,
                new HelidonResourceInfo(
                        interceptionContext.serviceInfo().serviceType().toString(),
                        interceptionContext.elementInfo().signature().toString()));
        if (allowed) {
            chain.proceed(request, response);
        }
    }
}
