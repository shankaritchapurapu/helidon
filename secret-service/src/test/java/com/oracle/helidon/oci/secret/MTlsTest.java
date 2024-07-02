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

package com.oracle.helidon.oci.secret;

import java.net.URI;
import java.time.Duration;

import io.helidon.common.config.GlobalConfig;
import io.helidon.common.tls.Tls;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import io.helidon.webclient.api.WebClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonTest
public class MTlsTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    MTlsTestRestClient restClient;

    private final URI uri;

    @Inject
    public MTlsTest(WebTarget target, @RestClient MTlsTestRestClient restClient) {
        this.uri = URI.create("https://localhost:" + target.getUri().getPort());
        this.restClient = restClient;
    }

    @Test
    public void webClient() {
        try (var res = WebClient.builder()
                .baseUri(uri)
                .config(GlobalConfig.config().get("client"))
                .connectTimeout(TIMEOUT)
                .readTimeout(TIMEOUT)
                .build()
                .get("/mtls-hello-world")
                .request()) {
            assertThat(res.as(String.class), is("MTls hello world"));
        }
    }

    @Test
    public void webTarget() {
        try (var res = ClientBuilder.newBuilder()
                .connectTimeout(TIMEOUT.getSeconds(), SECONDS)
                .readTimeout(TIMEOUT.getSeconds(), SECONDS)
                .sslContext(Tls.create(GlobalConfig.config().get("custom-client.tls")).sslContext())
                .build()
                .target(uri).path("/mtls-hello-world").request().get()) {
            assertThat(res.readEntity(String.class), is("MTls hello world"));
        }
    }

    @Test
    @Disabled("Not supported yet")
    public void restClientInjectedImplicit() {
        assertThat(restClient.getMtlsHelloWorld(), is("MTls hello world"));
    }

    @Test
    public void restClientBuilder() {
        MTlsTestRestClient explicitClient = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .sslContext(Tls.create(GlobalConfig.config().get("custom-client.tls")).sslContext())
                .build(MTlsTestRestClient.class);
        assertThat(explicitClient.getMtlsHelloWorld(), is("MTls hello world"));
    }

    @Path("/")
    public static class TestResource {
        @GET
        @Path("/mtls-hello-world")
        public String getMtlsHelloWorld() {
            return "MTls hello world";
        }
    }
}
