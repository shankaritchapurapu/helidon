/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;

import io.helidon.common.socket.PeerInfo;
import io.helidon.http.HeaderValues;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.WritableHeaders;
import io.helidon.webserver.http.ServerRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelidonRequestHeadersTest {

    @Test
    void shouldReturnExistingHeadersFromMultivaluedMap() {
        HelidonMultivaluedHashMap headers = new HelidonMultivaluedHashMap(mockRequest());

        assertEquals(List.of("first", "second"), headers.get("X-Test-Header"));
    }

    @Test
    void shouldReturnExistingHeadersIgnoringCase() {
        HelidonMultivaluedHashMap headers = new HelidonMultivaluedHashMap(mockRequest());

        assertEquals(List.of("first", "second"), headers.get("x-test-header"));
    }

    @Test
    void shouldReturnEmptyListForMissingAndNonStringHeaderKeys() {
        HelidonMultivaluedHashMap headers = new HelidonMultivaluedHashMap(mockRequest());

        assertTrue(headers.get("Missing").isEmpty());
        assertTrue(headers.get(42).isEmpty());
    }

    @Test
    void shouldExposeExistingHeadersThroughHttpHeaders() {
        HelidonHttpHeaders headers = new HelidonHttpHeaders(mockRequest());

        assertEquals(List.of("first", "second"), headers.getRequestHeader("X-Test-Header"));
        assertEquals("first", headers.getHeaderString("X-Test-Header"));
    }

    @Test
    void shouldExposeExistingHeadersThroughContainerRequestContext() {
        HelidonContainerRequestContext context =
                new HelidonContainerRequestContext(mockRequest(), new HelidonResourceInfo(
                        HelidonResourceInfoTest.SampleService.class.getName(),
                        "void doNothing()"));

        assertEquals(List.of("first", "second"), context.getHeaders().get("X-Test-Header"));
        assertEquals("first", context.getHeaderString("X-Test-Header"));
    }

    private static ServerRequest mockRequest() {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo peerInfo = mock(PeerInfo.class);
        ServerRequestHeaders headers = ServerRequestHeaders.create(WritableHeaders.create()
                .add(HeaderValues.create("X-Test-Header", "first", "second")));

        when(request.headers()).thenReturn(headers);
        when(request.remotePeer()).thenReturn(peerInfo);
        when(peerInfo.tlsCertificates()).thenReturn(Optional.<Certificate[]>empty());

        return request;
    }
}
