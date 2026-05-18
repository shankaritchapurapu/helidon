/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.tests.integration.identity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.http.signing.DefaultRequestSigner;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.http.signing.SigningStrategy;
import com.oracle.jipher.provider.JipherJCE;

import io.helidon.common.types.TypeName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdentityEndpointIT {
    private static final Logger LOGGER = Logger.getLogger(IdentityEndpointIT.class.getName());
    private static final TypeName INSTANCE_PRINCIPAL_METHOD_IMPL = TypeName.create(
            "io.helidon.integrations.oci.authentication.instance.AuthenticationMethodInstancePrincipal");
    private final String baseUri;

    static {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host,content-length");
    }

    public IdentityEndpointIT(WebServer webServer) {
        this.baseUri = "http://localhost:" + webServer.port();
    }

    @BeforeAll
    public static void setup() {
        Security.addProvider(new JipherJCE());
    }

    @Test
    @Order(1)
    void testPing() throws Exception {
        testPing(Status.OK_200.code(), "pong");
    }

    @Test
    @Order(2)
    void testOnceSuccess() throws Exception {
        testOnceSuccess(Status.OK_200.code(), "Hello World");
    }

    @Test
    @Order(3)
    void testTwiceSuccess() throws Exception {
        testTwiceSuccess(Status.OK_200.code(), "Hello WorldHello World");
    }

    Map<String, String> signRequest(URI uri,
                                    String httpMethod,
                                    Map<String, List<String>> headers,
                                    Object body) throws IOException {
        BasicAuthenticationDetailsProvider provider = Services.get(BasicAuthenticationDetailsProvider.class);
        RequestSigner signer = DefaultRequestSigner.createRequestSigner(provider);
        return signer.signRequest(uri, httpMethod, headers, body);
    }

    void testPing(int status, String body) throws Exception {
        URI uri = URI.create(this.baseUri + "/identity");
        HttpRequest.Builder request = HttpRequest.newBuilder(uri);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode(), is(status));
            if (status == Status.OK_200.code() && body != null) {
                assertThat(response.body(), is(body));
            }
        }
    }

    void testCall(int status, String body, String path) throws Exception {
        URI uri = URI.create(this.baseUri + path);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(HeaderNames.CONTENT_TYPE.defaultCase(), List.of(ContentType.TEXT_PLAIN.toString()));
        headers.put(HeaderNames.ACCEPT.defaultCase(), List.of(ContentType.TEXT_PLAIN.toString()));
        request.POST(HttpRequest.BodyPublishers.ofString("Hello World"));

        try {
            Map<String, String> signedHeaders = signRequest(uri, "POST", headers, "Hello World");
            signedHeaders.forEach(request::header);
        } catch (Exception e) {
            assertThat("Got Exception " + e.getMessage() + " while signing request, skipping test for " + path , false);
        }

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode(), is(status));
            if (status == Status.OK_200.code() && body != null) {
                assertThat(response.body(), is(body));
            }
        }
    }

    void testOnceSuccess(int status, String body) throws Exception {
        testCall(status, body, "/identity/once");
    }

    void testTwiceSuccess(int status, String body) throws Exception {
        testCall(status, body, "/identity/twice");
    }
}
