/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import io.helidon.common.socket.PeerInfo;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelidonContainerRequestFilterRunnerTest {

    @Test
    void shouldReturnContextWhenFilterAllowsRequest() throws Exception {
        ServerRequest request = mockRequest();
        ServerResponse response = mockResponse();

        Optional<HelidonContainerRequestContext> result = HelidonContainerRequestFilterRunner.run(
                context -> {
                },
                request,
                response,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertTrue(result.isPresent());
        verify(response, never()).send();
        verify(response, never()).send("blocked");
    }

    @Test
    void shouldSendResponseWhenFilterThrowsWebApplicationException() throws Exception {
        ServerRequest request = mockRequest();
        ServerResponse response = mockResponse();
        ContainerRequestFilter filter = context -> {
            throw new WebApplicationException(Response.status(403).entity("blocked").build());
        };

        Optional<HelidonContainerRequestContext> result = HelidonContainerRequestFilterRunner.run(
                filter,
                request,
                response,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertTrue(result.isEmpty());
        verify(response).status(403);
        verify(response).send("blocked");
    }

    @Test
    void shouldSendAbortResponseWhenFilterAborts() throws Exception {
        ServerRequest request = mockRequest();
        ServerResponse response = mockResponse();
        ContainerRequestFilter filter = context ->
                context.abortWith(Response.status(401).entity("denied").build());

        Optional<HelidonContainerRequestContext> result = HelidonContainerRequestFilterRunner.run(
                filter,
                request,
                response,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertTrue(result.isEmpty());
        verify(response).status(401);
        verify(response).send("denied");
    }

    private static ServerRequest mockRequest() {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo peerInfo = mock(PeerInfo.class);

        when(request.remotePeer()).thenReturn(peerInfo);
        when(peerInfo.tlsCertificates()).thenReturn(Optional.<Certificate[]>empty());
        when(request.headers()).thenReturn(ServerRequestHeaders.create());
        return request;
    }

    private static ServerResponse mockResponse() {
        ServerResponse response = mock(ServerResponse.class);
        when(response.status(anyInt())).thenReturn(response);
        return response;
    }
}
