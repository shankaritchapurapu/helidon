/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.pic.vault;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.ProcessingException;

import io.helidon.microprofile.testing.junit5.HelidonTest;

import com.oracle.bmc.auth.tls.TlsConfig;
import com.oracle.bmc.internal.client.http.OracleHttpClientConfig;
import com.oracle.oci.internals.sdk.http.internal.WithHeaders;
import com.oracle.pic.vault.model.GetSecretResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonTest
@Path("/")
public class JakartaJavaxCollisionTest {

    private static final String SECRET_PAYLOAD = """
            {
              "key": "-----BEGIN PRIVATE KEY-----\\nKEY-----END PRIVATE KEY-----\\n",
              "cert": "-----BEGIN CERTIFICATE-----\\nCERT-----END CERTIFICATE-----\\n",
              "intermediates": [
                "-----BEGIN CERTIFICATE-----\\nINTERMEDIATE1-----END CERTIFICATE-----\\n",
                "-----BEGIN CERTIFICATE-----\\nnINTERMEDIATE2-----END CERTIFICATE-----\\n"
              ]
            }
            """;

    @GET
    @Path("/fake/ssv2")
    @jakarta.ws.rs.Produces({"application/json"})
    public GetSecretResponse fakeSecretService() {
        return GetSecretResponse.builder()
                .data(Map.of("secret", Base64.getEncoder().encodeToString(SECRET_PAYLOAD.getBytes(StandardCharsets.UTF_8))))
                .build();
    }

    @GET
    @Path("/javax/err")
    @jakarta.ws.rs.Produces({"application/json"})
    public GetSecretResponse javaxErr() {
        throw new ProcessingException("BOOM");
    }

    @Test
    void callMockSsv2(WebTarget target) {
        SecretServiceConfig secretServiceConfig = SecretServiceConfig.builder()
                .endpoint("")
                .tlsConfig(TlsConfig.builder()
                                   .caBundle("target/test-classes/ca-bundle.pem")
                                   .build())
                .cacheConfig(CacheConfig.builder()
                                     .cacheType(CacheConfig.CacheType.IN_MEMORY_CACHE)
                                     .build())
                .retryConfig(RetryConfig.builder()
                                     .maxRetries(1)
                                     .build())
                .build();
        InternalVaultClient ivc = new InternalVaultClient(secretServiceConfig);

        OracleHttpClientConfig defaultConfig = OracleHttpClientConfig.defaultConfig(secretServiceConfig.getEndpoint());
        com.oracle.bmc.internal.client.retry.RetryConfig retryConfig =
                com.oracle.bmc.internal.client.retry.RetryConfig.builder()
                        .maxRetryDelayInMs(secretServiceConfig.getRetryConfig().getMaxRetryDelayInMs())
                        .maxRetries(secretServiceConfig.getRetryConfig().getMaxRetries()).retryableStatusCodeFamilies(
                                Arrays.asList(3, 5)).retryableStatusCodes(Collections.singletonList(429)).build();
        OracleHttpClientConfig oracleHttpClientConfig = OracleHttpClientConfig.builder()
                .connectTimeoutInMilliseconds(defaultConfig.getConnectTimeoutInMilliseconds())
                .readTimeoutInMilliseconds(defaultConfig.getReadTimeoutInMilliseconds())
                .asyncThreadPoolSize(defaultConfig.getAsyncThreadPoolSize()).endpoint(secretServiceConfig.getEndpoint())
                .retryConfig(retryConfig).tlsConfig(secretServiceConfig.getTlsConfig()).build();

        SecretRestClient restClient = new SecretRestClient(oracleHttpClientConfig, new MockAuthenticationDetailsProvider(), false);
        String url = target.getUriBuilder().path("/fake/ssv2").build().toASCIIString();
        target.path("/fake/ssv2").request().get().getEntity();
        WithHeaders<GetSecretResponse> secretWithHeaders = restClient.getSecretWithHeaders(url);
        GetSecretResponse secretResponse = secretWithHeaders.getItem();
        Map<String, String> rawSecret = secretResponse.getData();
        String json = new String(Base64.getDecoder().decode(rawSecret.get("secret").getBytes()), StandardCharsets.UTF_8);
        assertThat(json, is(SECRET_PAYLOAD));
    }

    @Test
    void javaxErrTest(WebTarget target) {
        target.path("/javax/err").request().get().readEntity(String.class);
    }
}
