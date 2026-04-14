/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import io.helidon.webserver.WebServer;

import com.oracle.jipher.provider.JipherJCE;
import com.oracle.pic.identity.authentication.AuthenticationClient;
import com.oracle.pic.identity.authentication.SignedRequestAuthenticationClient;
import com.oracle.pic.identity.authentication.key.WarnHardCodedRSAPrivateKeySupplier;
import com.oracle.pic.identity.authentication.key.WarnHardCodedRSAPublicKeySupplier;

/**
 * Shared helpers for sending signed requests against the example service.
 */
abstract class SignedRequestSupport {
    private final String baseUri;

    static {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host,content-length");
        Security.addProvider(new JipherJCE());
    }

    SignedRequestSupport(WebServer webServer) {
        this.baseUri = "http://localhost:" + webServer.port();
    }

    HttpResponse<String> signedJson(String path, String method, String body) throws Exception {
        return signedRequest(path, method, body, "application/json");
    }

    HttpResponse<String> signedDelete(String path) throws Exception {
        return signedRequest(path, "DELETE", null, "application/json");
    }

    private HttpResponse<String> signedRequest(String path, String method, String body, String mediaType) throws Exception {
        URI uri = URI.create(baseUri + path);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .method(method,
                        body == null
                                ? HttpRequest.BodyPublishers.noBody()
                                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        Map<String, List<String>> headers = new java.util.LinkedHashMap<>();
        headers.put("Accept", List.of(mediaType));
        if (body != null) {
            headers.put("Content-Type", List.of(mediaType));
        }

        signedHeaders(uri, method, headers, body).forEach(request::header);

        HttpClient client = HttpClient.newHttpClient();
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, String> signedHeaders(URI uri,
                                                     String method,
                                                     Map<String, List<String>> headers,
                                                     String body) throws Exception {
        RSAPublicKey publicKey = new WarnHardCodedRSAPublicKeySupplier().getKey("foo").orElseThrow();
        String tenantId = "ocid1.certificate.oc1..aaaaaaaabbbbbbbbccccccccdddddddd";
        String userId = "ocid1.user.oc1..aaaaaaaahrvnng6zwht4g2cfmxpehakhzqggcfekrxnux5lzwnwxdkopebaq";
        SignedRequestAuthenticationClient signerClient =
                new AuthenticationClient.SignedRequestBuilder(new WarnHardCodedRSAPrivateKeySupplier()).build();
        return signerClient.getSignedRequestHeaders(tenantId + "/" + userId + "/" + computeFingerprint(publicKey),
                                                    method,
                                                    uri,
                                                    headers,
                                                    body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
    }

    private static String computeFingerprint(RSAPublicKey key) throws Exception {
        byte[] spki = key.getEncoded();
        byte[] digest = MessageDigest.getInstance("MD5").digest(spki);
        StringBuilder result = new StringBuilder(digest.length * 3);
        for (int i = 0; i < digest.length; i++) {
            result.append(String.format("%02x", digest[i]));
            if (i < digest.length - 1) {
                result.append(':');
            }
        }
        return result.toString();
    }
}
