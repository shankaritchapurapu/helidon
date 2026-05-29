/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.identity;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import com.oracle.pic.identity.authentication.AuthenticationClient;
import com.oracle.pic.identity.authentication.SignedRequestAuthenticationClient;
import com.oracle.pic.identity.authentication.key.WarnHardCodedRSAPrivateKeySupplier;
import com.oracle.pic.identity.authentication.key.WarnHardCodedRSAPublicKeySupplier;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@ServerTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IdentityEndpointHardCodedTest extends IdentityEndpointBase {

    static {
        // needs to run in a fresh Java VM
        System.setProperty("helidon.config.profile", "test");
    }

    public IdentityEndpointHardCodedTest(WebServer webServer) {
        super(webServer);
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
                                    Object body) throws Exception {
        RSAPublicKey publicKey = new WarnHardCodedRSAPublicKeySupplier().getKey("foo").orElseThrow();
        String tenantId = "ocid1.certificate.oc1..aaaaaaaabbbbbbbbccccccccdddddddd";
        String userId = "ocid1.user.oc1..aaaaaaaahrvnng6zwht4g2cfmxpehakhzqggcfekrxnux5lzwnwxdkopebaq";
        SignedRequestAuthenticationClient signerClient =
                new AuthenticationClient.SignedRequestBuilder(new WarnHardCodedRSAPrivateKeySupplier()).build();
        return signerClient.getSignedRequestHeaders(tenantId + "/" + userId + "/" + computeFingerprint(publicKey),
                                                    httpMethod,
                                                    uri,
                                                    headers,
                                                    readBodyBytes(body));
    }
}
