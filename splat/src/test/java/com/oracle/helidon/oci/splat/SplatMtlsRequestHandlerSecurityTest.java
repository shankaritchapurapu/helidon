/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.Optional;

import javax.ws.rs.container.ResourceInfo;

import io.helidon.common.uri.UriInfo;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Method;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.WritableHeaders;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SplatMtlsRequestHandlerSecurityTest {

    @Test
    void shouldRejectInsecureRequestWithoutClientCertificate() throws Exception {
        SplatMtlsRequestHandler handler = handler(SplatMtlsConfig.builder()
                                                         .region("us-ashburn-1")
                                                         .buildPrototype());
        ServerRequest request = mockRequest(false, "http", ServerRequestHeaders.create());
        ServerResponse response = mockResponse();

        boolean allowed = handler.shouldAllow(request, response, mock(ResourceInfo.class));

        assertFalse(allowed);
        verify(response).status(403);
    }

    @Test
    void shouldRejectSpoofedHttpSchemeWithoutClientCertificate() throws Exception {
        SplatMtlsRequestHandler handler = handler(SplatMtlsConfig.builder()
                                                         .region("us-ashburn-1")
                                                         .buildPrototype());
        ServerRequest request = mockRequest(true, "http", ServerRequestHeaders.create());
        ServerResponse response = mockResponse();

        boolean allowed = handler.shouldAllow(request, response, mock(ResourceInfo.class));

        assertFalse(allowed);
        verify(response).status(403);
    }

    @Test
    void shouldRejectCommonNameNotOnAllowList() throws Exception {
        SplatMtlsRequestHandler handler = handler(SplatMtlsConfig.builder()
                                                         .region("us-ashburn-1")
                                                         .skipAuthzValidationCheck(true)
                                                         .buildPrototype());
        ServerRequest request = mockRequestWithCommonName(true, "https", "attacker.example");
        ServerResponse response = mockResponse();

        boolean allowed = handler.shouldAllow(request, response, mock(ResourceInfo.class));

        assertFalse(allowed);
        verify(response).status(403);
    }

    @Test
    void shouldRejectBareSplatClientWhenRejectingCrossRegionCalls() throws Exception {
        SplatMtlsRequestHandler handler = handler(SplatMtlsConfig.builder()
                                                         .region("us-ashburn-1")
                                                         .rejectXRegionCalls(true)
                                                         .skipAuthzValidationCheck(true)
                                                         .buildPrototype());
        ServerRequest request = mockRequestWithCommonName(true, "https", "splat-client");
        ServerResponse response = mockResponse();

        boolean allowed = handler.shouldAllow(request, response, mock(ResourceInfo.class));

        assertFalse(allowed);
        verify(response).status(403);
    }

    @Test
    void shouldAllowRegionalSplatApiClientCommonName() throws Exception {
        SplatMtlsConfig config = SplatMtlsConfig.builder()
                .region("us-ashburn-1")
                .rejectXRegionCalls(true)
                .skipAuthzValidationCheck(true)
                .buildPrototype();
        SplatMtlsRequestValidator validator = new SplatMtlsRequestValidator(config);
        ServerRequest request = mockRequestWithCommonName(true,
                                                          "https",
                                                          "splat-api-client.us-ashburn-1.oci.oracleiaas.com");

        Optional<String> validationFailure = validator.validate(request, Region.fromPublicRegionName("us-ashburn-1"));

        assertTrue(validationFailure.isEmpty(), validationFailure::orElseThrow);
    }

    @Test
    void shouldRejectUnsignedOpcPrincipalAuthzSkip() throws Exception {
        SplatMtlsRequestHandler handler = handler(SplatMtlsConfig.builder()
                                                         .region("us-ashburn-1")
                                                         .rejectXRegionCalls(true)
                                                         .buildPrototype());
        ServerRequestHeaders headers = ServerRequestHeaders.create(WritableHeaders.create()
                .add(HeaderValues.create(HeaderNames.X_HELIDON_CN,
                                         "splat-api-client.us-ashburn-1.oci.oracleiaas.com"))
                .add(HeaderValues.create("opc-principal",
                                         """
                                         {
                                           "subjectId": "splat",
                                           "tenantId": "tenant",
                                           "claims": [
                                             {
                                               "key": "svc",
                                               "value": "splat",
                                               "issuer": "splat"
                                             }
                                           ]
                                         }
                                         """)));
        ServerRequest request = mockRequest(true, "https", headers);
        ServerResponse response = mockResponse();

        boolean allowed = handler.shouldAllow(request, response, mock(ResourceInfo.class));

        assertFalse(allowed);
        verify(response).status(403);
    }

    private static SplatMtlsRequestHandler handler(SplatMtlsConfig config) {
        return new SplatMtlsRequestHandler(config,
                                           () -> {
                                               throw new AssertionError("Default region should not be resolved");
                                           });
    }

    private static ServerRequest mockRequestWithCommonName(boolean secure, String scheme, String commonName) {
        ServerRequestHeaders headers = ServerRequestHeaders.create(WritableHeaders.create()
                .add(HeaderValues.create(HeaderNames.X_HELIDON_CN, commonName)));
        return mockRequest(secure, scheme, headers);
    }

    private static ServerRequest mockRequest(boolean secure,
                                             String scheme,
                                             ServerRequestHeaders headers) {
        ServerRequest request = mock(ServerRequest.class);

        when(request.isSecure()).thenReturn(secure);
        when(request.prologue()).thenReturn(HttpPrologue.create("HTTP/1.1", "HTTP", "1.1", Method.GET, "/splat", false));
        when(request.requestedUri()).thenReturn(UriInfo.builder()
                                                     .scheme(scheme)
                                                     .host("localhost")
                                                     .path("/splat")
                                                     .buildPrototype());
        when(request.headers()).thenReturn(headers);
        return request;
    }

    private static ServerResponse mockResponse() {
        ServerResponse response = mock(ServerResponse.class);
        when(response.status(anyInt())).thenReturn(response);
        return response;
    }
}
