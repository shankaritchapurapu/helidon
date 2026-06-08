/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.errorcode.webserver;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.HttpEntryPoint;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.oracle.helidon.oci.errorcode.RenderableException;

/**
 * Maps OCI renderable exceptions thrown by Helidon declarative SE endpoints.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 100)
class ErrorCodeHttpEntryPointInterceptor implements HttpEntryPoint.Interceptor {
    private static final String FEATURE_CONFIG = "server.features." + ErrorCodeServerFeature.TYPE;

    private final boolean enabled;

    @Service.Inject
    ErrorCodeHttpEntryPointInterceptor(Config config) {
        this.enabled = enabled(config);
    }

    static boolean enabled(Config config) {
        return config.get(FEATURE_CONFIG)
                .get("enabled")
                .asBoolean()
                .orElse(true);
    }

    @Override
    public void proceed(InterceptionContext interceptionContext,
                        Chain chain,
                        ServerRequest request,
                        ServerResponse response) throws Exception {
        try {
            chain.proceed(request, response);
        } catch (RenderableException e) {
            if (!enabled) {
                throw e;
            }
            new ErrorCodeServerFeature.RenderableErrorHandler().handle(request, response, e);
        }
    }
}
