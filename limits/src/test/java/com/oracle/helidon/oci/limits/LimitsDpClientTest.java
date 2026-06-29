/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.HeaderNames;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.oci.limits.LimitsDPClient;
import com.oracle.oci.limits.requests.EvaluateLimitForAdRequest;
import com.oracle.oci.limits.responses.EvaluateLimitForAdResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ServerTest
class LimitsDpClientTest {
    private static final String EVALUATE_LIMIT_ROUTE = "/20180322/limits/evaluate/group/{group}/limit/{limit}/"
            + "tag/{tag}/value/{value}/region/{region}/ad/{ad}";

    private final LimitsDPClient client;

    LimitsDpClientTest(WebServer server) throws Exception {
        LimitsConfig limitsConfig = LimitsConfig.builder()
                .endpoint("http://localhost:" + server.port())
                .build();
        BasicAuthenticationDetailsProvider authProvider = authProvider();
        LimitsAuthProviderFactory authProviderFactory = new LimitsAuthProviderFactory(
                limitsConfig,
                OciConfig.builder().build(),
                () -> Optional.of(authProvider));
        this.client = new LimitsDpClientFactory(limitsConfig, authProviderFactory).get();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        builder.get(EVALUATE_LIMIT_ROUTE, (request, response) -> {
            assertTrue(request.headers().contains(HeaderNames.AUTHORIZATION),
                       "Limits request should be signed");
            response.header(HeaderNames.CONTENT_TYPE_NAME, MediaTypes.APPLICATION_JSON_VALUE)
                    .send("true");
        });
    }

    @Test
    void evaluateLimitForAdUsesConfiguredEndpointAndDeserializesResponse() {
        EvaluateLimitForAdRequest request = EvaluateLimitForAdRequest.builder()
                .ad("ad1")
                .value("val1")
                .region("region1")
                .limit("20")
                .compartmentId("c1")
                .tag("tag1")
                .group("group1")
                .build();

        EvaluateLimitForAdResponse response = client.evaluateLimitForAd(request);
        assertEquals(Boolean.TRUE, response.getValue());
    }

    private static BasicAuthenticationDetailsProvider authProvider() throws Exception {
        byte[] privateKey = privateKey();
        return new BasicAuthenticationDetailsProvider() {
            @Override
            public String getKeyId() {
                return "ocid1.tenancy.oc1..test/ocid1.user.oc1..test/fingerprint";
            }

            @Override
            public InputStream getPrivateKey() {
                return new ByteArrayInputStream(privateKey);
            }

            @Override
            public String getPassPhrase() {
                return null;
            }

            @Override
            public char[] getPassphraseCharacters() {
                return null;
            }
        };
    }

    private static byte[] privateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        byte[] encoded = generator.generateKeyPair().getPrivate().getEncoded();
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encoded);
        return ("-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n")
                .getBytes(StandardCharsets.US_ASCII);
    }
}
