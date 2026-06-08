/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.errorcode.webserver;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.Service;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpEntryPoint;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import io.helidon.webserver.testing.junit5.Socket;

import com.oracle.helidon.oci.errorcode.ErrorCodes;
import com.oracle.helidon.oci.errorcode.ErrorDetail;
import com.oracle.helidon.oci.errorcode.RenderableException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class ServiceRegistryErrorCodeTest {
    private static final String NAMED_SOCKET = "named";

    private final Http1Client client;
    private final Http1Client namedClient;

    ServiceRegistryErrorCodeTest(Http1Client client, @Socket(NAMED_SOCKET) Http1Client namedClient) {
        this.client = client;
        this.namedClient = namedClient;
    }

    @SetUpRoute(NAMED_SOCKET)
    static void namedRouting(HttpRules rules) {
        rules.get("/named-socket-error",
                  (req, res) -> {
                      throw new RenderableException(ErrorCodes.InvalidParameter,
                                                    "Invalid parameter sent from named socket route");
                  });
    }

    @Test
    void testServiceRegistryFeature() {
        ClientResponseTyped<ErrorDetail> response = client.get("/nested/service-registry-error")
                .request(ErrorDetail.class);

        assertThat(response.status(), is(ErrorCodes.InvalidParameter.status()));
        ErrorDetail errorDetail = response.entity();
        assertThat(errorDetail.getErrorCode(), is(ErrorCodes.InvalidParameter.errorCode()));
        assertThat(errorDetail.getMessage(), is("Invalid parameter sent from service-registry route"));
    }

    @Test
    void testServiceRegistryFeatureWithNamedSocket() {
        ClientResponseTyped<ErrorDetail> response = namedClient.get("/named-socket-error")
                .request(ErrorDetail.class);

        assertThat(response.status(), is(ErrorCodes.InvalidParameter.status()));
        ErrorDetail errorDetail = response.entity();
        assertThat(errorDetail.getErrorCode(), is(ErrorCodes.InvalidParameter.errorCode()));
        assertThat(errorDetail.getMessage(), is("Invalid parameter sent from named socket route"));
    }

    @Test
    void testServiceRegistryFeatureWithDeclarativeEndpoint() {
        ClientResponseTyped<ErrorDetail> response = client.get("/declarative/service-registry-error")
                .request(ErrorDetail.class);

        assertThat(response.status(), is(ErrorCodes.InvalidParameter.status()));
        ErrorDetail errorDetail = response.entity();
        assertThat(errorDetail.getErrorCode(), is(ErrorCodes.InvalidParameter.errorCode()));
        assertThat(errorDetail.getMessage(), is("Invalid parameter sent from declarative endpoint"));
    }

    @Test
    void testEnabledByDefault() {
        assertThat(ErrorCodeHttpEntryPointInterceptor.enabled(config(Map.of())), is(true));
    }

    @Test
    void testDisabledByConfig() {
        assertThat(ErrorCodeHttpEntryPointInterceptor.enabled(config(Map.of(
                "server.features.oci-error-code.enabled", "false"))), is(false));
    }

    @Service.Singleton
    static class ThrowingHttpFeature implements HttpFeature {
        @Override
        public void setup(HttpRouting.Builder routing) {
            routing.register("/nested",
                             rules -> rules.get("/service-registry-error",
                                                (req, res) -> {
                                                    throw new RenderableException(
                                                            ErrorCodes.InvalidParameter,
                                                            "Invalid parameter sent from service-registry route");
                                                }));
        }
    }

    @Service.Singleton
    static class PassThroughEntryPointInterceptor implements HttpEntryPoint.Interceptor {
        @Override
        public void proceed(InterceptionContext interceptionContext,
                            Chain chain,
                            ServerRequest request,
                            ServerResponse response) throws Exception {
            chain.proceed(request, response);
        }
    }

    private static Config config(Map<String, String> values) {
        return Config.just(ConfigSources.create(values));
    }
}
