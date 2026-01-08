/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.HeaderName;
import io.helidon.http.Status;
import io.helidon.service.registry.Services;
import io.helidon.webclient.http1.Http1ClientRequest;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpFeatures;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@RoutingTest
class SplatMtlsFeatureTest {
    static final String VALID_SPLAT_CERTIFICATE_CN = "splat-api-client.us-ashburn-1.oci.oracleiaas.com";
    static final String INVALID_SPLAT_CERTIFICATE_CN = "invalid.anywhere-1.oci.oracleiaas.com";
    // minimum "opc-principal" header value that is sufficient to indicate that splat has done authorization
    private static final String AUTHORIZED_OPC_PRINCIPAL_VALUE =
            "{\"subjectId\":\"splat\",\"tenantId\":\"ocid1.tenancy.oc1..mydummytenant\"," +
            "\"claims\":[{\"key\":\"svc\",\"value\":\"splat\",\"issuer\":\"splat\"}]}";
    private final DirectClient client;
    private Map<HeaderName, String> headers;

    SplatMtlsFeatureTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        router.get("/get", (req, res) -> res.status(Status.OK_200).send());
    }

    @SetUpFeatures
    static List<ServerFeature> features() {
        Config config = Config.just(ConfigSources.create(
                Map.of("oci.splat.region", "us-ashburn-1")));
        Services.set(Config.class, config);
        return List.of(Services.get(SplatMtlsFeature.class));
    }

    @Test
    void testHTTPSkipCplatValidation() {
        client.setTls(false);
        Http1ClientResponse response = client.get("/get").request();
        assertThat(response.status(), is(Status.OK_200));
    }

    @ParameterizedTest
    @MethodSource("requestParameters")
    void testSplatValidation(
            String clientCertificateCN, Map<HeaderName, String> headers, Status status, String expectedResponse) {
        client.setTls(true);
        if (clientCertificateCN != null) {
            client.clientTlsCertificates(new Certificate[] {getClientTlsCertificate(clientCertificateCN)});
        }
        Http1ClientRequest request = client.get("/get");
        if (headers != null) {
            headers.forEach(request::header);
        }
        Http1ClientResponse response = request.request();

        assertThat(response.status(), is(status));
        if (expectedResponse != null) {
            assertThat(response.entity().as(String.class), is(expectedResponse));
        }
    }

    private static Stream<Arguments> requestParameters() {
        return Stream.of(
                // No certificate
                arguments(null,
                          null,
                          Status.FORBIDDEN_403,
                          "No client certificate was found"),
                // Invalid Certificate CN
                arguments(INVALID_SPLAT_CERTIFICATE_CN,
                          null,
                          Status.FORBIDDEN_403,
                          "Client cert is not whitelisted"),
                // Valid Certificate CN, but requirements for validation are not complete
                arguments(VALID_SPLAT_CERTIFICATE_CN,
                          null,
                          Status.FORBIDDEN_403,
                          "Authz has not been enabled at Splat"),
                // Splat instructs validation to be skipped using a request header
                arguments(VALID_SPLAT_CERTIFICATE_CN,
                          Map.of(SplatMtlsFilter.SKIP_AUTHORIZATION_FOR_SPLAT_HEADER_NAME, "true"),
                          Status.OK_200,
                          null),
                // All splat validation requirements are met
                arguments(VALID_SPLAT_CERTIFICATE_CN,
                          Map.of(SplatMtlsFilter.PRINCIPAL_OPC_HEADER_NAME, AUTHORIZED_OPC_PRINCIPAL_VALUE),
                          Status.OK_200,
                          null)
        );
    }

    static Certificate getClientTlsCertificate(String certCN) {
        X509Certificate certificate = Mockito.mock(X509Certificate.class);
        X500Principal principal = new X500Principal("CN=" + certCN);
        when(certificate.getSubjectX500Principal()).thenReturn(principal);
        return certificate;
    }
}
