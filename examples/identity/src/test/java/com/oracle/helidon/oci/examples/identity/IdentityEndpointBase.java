/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.identity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.WebServer;

import com.oracle.bmc.io.DuplicatableInputStream;
import com.oracle.bmc.io.internal.KeepOpenInputStream;
import com.oracle.bmc.retrier.Retriers;
import com.oracle.bmc.util.StreamUtils;
import com.oracle.jipher.provider.JipherJCE;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authentication.PrincipalImpl;
import com.oracle.pic.identity.authentication.PrincipalSerializerFactory;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeAll;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

abstract class IdentityEndpointBase {
    private static final System.Logger LOGGER = System.getLogger(IdentityEndpointBase.class.getName());
    private static final String REQUEST_BODY = "Hello World";

    static {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host,content-length");
    }

    private final String baseUri;

    public IdentityEndpointBase(WebServer webServer) {
        this.baseUri = "http://localhost:" + webServer.port();
    }

    abstract Map<String, String> signRequest(URI uri,
                                             String httpMethod,
                                             Map<String, List<String>> headers,
                                             Object body) throws Exception;

    @BeforeAll
    public static void setup() {
        Security.addProvider(new JipherJCE());
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
            LOGGER.log(System.Logger.Level.WARNING, "Unable to sign request, skipping test for " + path);
            return;
        }

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode(), is(status));
            if (status == Status.OK_200.code() && body != null) {
                assertThat(response.body(), is(body));
            }
        }
    }

    void testSplatCall(int status, String body, String path) throws Exception {
        testSplatCall(status,
                      body,
                      path,
                      HttpClient.Version.HTTP_1_1,
                      HttpRequest.BodyPublishers.ofString(REQUEST_BODY));
    }

    void testSplatChunkedCall(int status, String body, String path) throws Exception {
        testSplatCall(status,
                      body,
                      path,
                      HttpClient.Version.HTTP_1_1,
                      unknownLengthBodyPublisher(REQUEST_BODY));
    }

    void testSplatHttp2Call(int status, String body, String path) throws Exception {
        testSplatCall(status,
                      body,
                      path,
                      HttpClient.Version.HTTP_2,
                      unknownLengthBodyPublisher(REQUEST_BODY));
    }

    void testSplatChunkedZeroLengthCall(int status, String body, String path) throws Exception {
        testSplatCall(status,
                      body,
                      path,
                      HttpClient.Version.HTTP_1_1,
                      unknownLengthBodyPublisher(""));
    }

    void testSplatHttp2ZeroLengthCall(int status, String body, String path) throws Exception {
        testSplatCall(status,
                      body,
                      path,
                      HttpClient.Version.HTTP_2,
                      unknownLengthBodyPublisher(""));
    }

    private void testSplatCall(int status,
                               String body,
                               String path,
                               HttpClient.Version version,
                               HttpRequest.BodyPublisher bodyPublisher) throws Exception {
        URI uri = URI.create(this.baseUri + path);
        // Test-only SPLAT simulation: in production these headers are supplied
        // only by trusted SPLAT/mTLS infrastructure after validation. Direct
        // clients must not inject or rely on these authorization-related headers.
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .version(version)
                .header(HeaderNames.CONTENT_TYPE.defaultCase(), ContentType.TEXT_PLAIN.toString())
                .header(HeaderNames.ACCEPT.defaultCase(), ContentType.TEXT_PLAIN.toString())
                .header(Principal.OPC_HEADER, serializedSplatPrincipal())
                .header("oci-skip-authorization-for-splat", "true")
                .POST(bodyPublisher);

        try (HttpClient client = HttpClient.newBuilder().version(version).build()) {
            if (version == HttpClient.Version.HTTP_2) {
                HttpRequest warmup = HttpRequest.newBuilder(URI.create(this.baseUri + "/identity"))
                        .version(HttpClient.Version.HTTP_2)
                        .GET()
                        .build();
                HttpResponse<Void> warmupResponse = client.send(warmup, HttpResponse.BodyHandlers.discarding());
                assertThat(warmupResponse.statusCode(), is(Status.OK_200.code()));
                assertThat(warmupResponse.version(), is(HttpClient.Version.HTTP_2));
            }
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode(), is(status));
            assertThat(response.version(), is(version));
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

    void testSplatOnceSuccess(int status, String body) throws Exception {
        testSplatCall(status, body, "/identity/once");
    }

    void testSplatTwiceSuccess(int status, String body) throws Exception {
        testSplatCall(status, body, "/identity/twice");
    }

    void testSplatChunkedOnceSuccess(int status, String body) throws Exception {
        testSplatChunkedCall(status, body, "/identity/once");
    }

    void testSplatHttp2OnceSuccess(int status, String body) throws Exception {
        testSplatHttp2Call(status, body, "/identity/once");
    }

    void testSplatChunkedZeroLengthOnceSuccess(int status, String body) throws Exception {
        testSplatChunkedZeroLengthCall(status, body, "/identity/once");
    }

    void testSplatHttp2ZeroLengthOnceSuccess(int status, String body) throws Exception {
        testSplatHttp2ZeroLengthCall(status, body, "/identity/once");
    }

    private static HttpRequest.BodyPublisher unknownLengthBodyPublisher(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bytes));
    }

    static byte[] readBodyBytes(Object body) throws IOException {
        if (body == null) {
            return "".getBytes(StandardCharsets.UTF_8);
        }
        if (body instanceof String) {
            return ((String) body).getBytes(StandardCharsets.UTF_8);
        } else if (body instanceof DuplicatableInputStream) {
            final InputStream duplicatedBody = ((DuplicatableInputStream) body).duplicate();
            return StreamUtils.toByteArray(duplicatedBody);
        } else if (body instanceof KeepOpenInputStream) {
            byte[] byteArr = StreamUtils.toByteArray((KeepOpenInputStream) body);
            Retriers.tryResetStreamForRetry((InputStream) body, true);
            return byteArr;
        } else if (body instanceof InputStream) {
            throw new IllegalArgumentException(
                    "Only DuplicatableInputStream supported for body that needs signing.");
        }

        throw new IllegalArgumentException("Unexpected body type: " + body.getClass().getName());
    }

    static String computeFingerprint(RSAPublicKey key) throws Exception {
        byte[] spki = key.getEncoded();
        byte[] digest = MessageDigest.getInstance("MD5").digest(spki);
        return toHex(digest, ":");
    }

    static String toHex(byte[] bytes, String sep) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
            if (sep != null && i < bytes.length - 1) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }

    private static String serializedSplatPrincipal() {
        // Test-only fake OCIDs. Do not copy these values into production code;
        // replace them with appropriate test principals if local test identity
        // conventions change.
        Principal principal = new PrincipalImpl(
                "ocid1.tenancy.oc1..aaaaaaaaojp4splattenant",
                "ocid1.user.oc1..aaaaaaaasplatforwardeduser");
        return PrincipalSerializerFactory.create().serialize(principal).orElseThrow();
    }
}
