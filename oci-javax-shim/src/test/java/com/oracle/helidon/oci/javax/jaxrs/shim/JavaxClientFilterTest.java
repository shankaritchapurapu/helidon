/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.javax.jaxrs.shim;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.client.ClientBuilder;

import io.helidon.microprofile.testing.junit5.HelidonTest;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.RequestInterceptor;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

;

@HelidonTest
@Path("/")
public class JavaxClientFilterTest {

    private final URI serverUri;

    @Inject
    public JavaxClientFilterTest(WebTarget target) {
        serverUri = target.getUri();
    }

    static Stream<Registrar> javaxBuilderJavaxFilterSource() {
        return Stream.of(
                new Registrar("object", b -> b.register(new JavaxClientRequestFilter())),
                new Registrar("class", b -> b.register(JavaxClientRequestFilter.class)),
                new Registrar("object, priority", b -> b.register(new JavaxClientRequestFilter(), 1)),
                new Registrar("class, priority", b -> b.register(JavaxClientRequestFilter.class, 1))
        );
    }

    static Stream<Registrar> javaxBuilderJakartaFilterSource() {
        return Stream.of(
                new Registrar("object", b -> b.register(new JakartaClientRequestFilter())),
                new Registrar("class", b -> b.register(JakartaClientRequestFilter.class)),
                new Registrar("object, priority", b -> b.register(new JakartaClientRequestFilter(), 1)),
                new Registrar("class, priority", b -> b.register(JakartaClientRequestFilter.class, 1))
        );
    }

    static Stream<Registrar> javaxBuilderOciFilterSource() {
        return Stream.of(
                new Registrar("object", b -> b.register(new OciRequestInterceptor())),
                new Registrar("class", b -> b.register(OciRequestInterceptor.class)),
                new Registrar("object, priority", b -> b.register(new OciRequestInterceptor(), 1)),
                new Registrar("class, priority", b -> b.register(OciRequestInterceptor.class, 1))
        );
    }

    @GET
    @Path("/test1")
    public Response test1(@HeaderParam("client-filter-header") String header) {
        return Response.ok("Frank says he's got a header from " + header).build();
    }

    @ParameterizedTest
    @MethodSource("javaxBuilderJavaxFilterSource")
    void javaxClientWithJavaxFilter(Registrar registrar) {
        javax.ws.rs.client.WebTarget javaxTarget = registrar.fn().apply(javax.ws.rs.client.ClientBuilder.newBuilder())
                .build()
                .target(serverUri);

        assertThat(javaxTarget.path("/test1").request().get().readEntity(String.class),
                   is("Frank says he's got a header from Javax client req filter!"));
    }

    @ParameterizedTest
    @MethodSource("javaxBuilderJakartaFilterSource")
    void javaxClientWithJakartaFilter(Registrar registrar) {
        javax.ws.rs.client.WebTarget javaxTarget = registrar.fn().apply(javax.ws.rs.client.ClientBuilder.newBuilder())
                .build()
                .target(serverUri);

        assertThat(javaxTarget.path("/test1").request().get().readEntity(String.class),
                   is("Frank says he's got a header from Jakarta client req filter!"));
    }

    @ParameterizedTest
    @MethodSource("javaxBuilderJakartaFilterSource")
    void javaxClientWithJakartaFilterRx(Registrar registrar) throws ExecutionException, InterruptedException, TimeoutException {
        javax.ws.rs.client.WebTarget javaxTarget = registrar.fn().apply(javax.ws.rs.client.ClientBuilder.newBuilder())
                .build()
                .target(serverUri);

        assertThat(javaxTarget.path("/test1")
                           .request()
                           .rx()
                           .get(String.class)
                           .toCompletableFuture()
                           .get(20, TimeUnit.SECONDS),
                   is("Frank says he's got a header from Jakarta client req filter!"));
    }

    @ParameterizedTest
    @MethodSource("javaxBuilderOciFilterSource")
    void javaxClientWithOciFilter(Registrar registrar) {
        javax.ws.rs.client.WebTarget javaxTarget = registrar.fn().apply(ClientBuilder.newBuilder())
                .build()
                .target(serverUri);

        assertThat(javaxTarget.path("/test1").request().get().readEntity(String.class),
                   is("Frank says he's got a header from OCI 3.x client req interceptor!"));
    }

    @Test
    void jakartaClientWithJakartaFilter() {
        jakarta.ws.rs.client.WebTarget jakartaTarget = jakarta.ws.rs.client.ClientBuilder.newBuilder()
                .register(JakartaClientRequestFilter.class)
                .build()
                .target(serverUri);

        assertThat(jakartaTarget.path("/test1").request().get().readEntity(String.class),
                   is("Frank says he's got a header from Jakarta client req filter!"));
    }

    @Test
    @Disabled("Not needed yet")
    void jakartaClientWithJavaxFilter() {
        // Implementation would require different approach
    }

    public static class JavaxClientRequestFilter implements javax.ws.rs.client.ClientRequestFilter {
        @Override
        public void filter(javax.ws.rs.client.ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().add("client-filter-header", "Javax client req filter!");
        }
    }

    public static class JakartaClientRequestFilter implements jakarta.ws.rs.client.ClientRequestFilter {
        @Override
        public void filter(jakarta.ws.rs.client.ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().add("client-filter-header", "Jakarta client req filter!");
        }
    }

    public static class OciRequestInterceptor implements RequestInterceptor {
        @Override
        public void intercept(HttpRequest req) {
            req.header("client-filter-header", "OCI 3.x client req interceptor!");
        }
    }

    record Registrar(String name, Function<ClientBuilder, ClientBuilder> fn) {
        @Override
        public String toString() {
            return name;
        }
    }
}
