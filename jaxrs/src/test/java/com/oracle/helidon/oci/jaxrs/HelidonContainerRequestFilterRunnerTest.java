/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import io.helidon.common.GenericType;
import io.helidon.common.buffers.BufferData;
import io.helidon.common.socket.PeerInfo;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.media.ReadableEntity;
import io.helidon.http.media.ReadableEntityBase;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelidonContainerRequestFilterRunnerTest {

    @Test
    void shouldBufferAndReplayZeroLengthEntity() throws Exception {
        ReadableEntity entity = new ZeroLengthReadableEntity();
        ServerRequest request = mockRequest(entity);
        ServerResponse response = mockResponse();
        AtomicBoolean filterInvoked = new AtomicBoolean();

        Optional<HelidonContainerRequestContext> result = HelidonContainerRequestFilterRunner.run(
                context -> {
                    filterInvoked.set(true);
                    assertTrue(context.hasEntity());
                    assertArrayEquals(new byte[0], context.getEntityStream().readAllBytes());
                },
                request,
                response,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertTrue(result.isPresent());
        assertTrue(filterInvoked.get());
        assertArrayEquals(new byte[0], request.content().inputStream().readAllBytes());
    }

    @Test
    void shouldBufferEntityWithoutContentLength() throws Exception {
        ReadableEntity entity = mock(ReadableEntity.class);
        ServerRequest request = mockRequest(entity);
        ServerResponse response = mockResponse();

        when(entity.hasEntity()).thenReturn(true);

        Optional<HelidonContainerRequestContext> result = HelidonContainerRequestFilterRunner.run(
                context -> assertTrue(context.hasEntity()),
                request,
                response,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertTrue(result.isPresent());
        verify(entity).buffer();
    }

    @Test
    void shouldNotBufferBodylessRequest() throws Exception {
        ReadableEntity entity = mock(ReadableEntity.class);
        ServerRequest request = mockRequest(entity);
        ServerResponse response = mockResponse();

        when(entity.hasEntity()).thenReturn(false);

        Optional<HelidonContainerRequestContext> result = HelidonContainerRequestFilterRunner.run(
                context -> {
                },
                request,
                response,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertTrue(result.isPresent());
        verify(entity, never()).buffer();
    }

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
        ReadableEntity entity = mock(ReadableEntity.class);
        return mockRequest(entity);
    }

    private static ServerRequest mockRequest(ReadableEntity entity) {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo peerInfo = mock(PeerInfo.class);

        when(request.remotePeer()).thenReturn(peerInfo);
        when(peerInfo.tlsCertificates()).thenReturn(Optional.<Certificate[]>empty());
        when(request.headers()).thenReturn(ServerRequestHeaders.create());
        when(request.content()).thenReturn(entity);
        return request;
    }

    private static ServerResponse mockResponse() {
        ServerResponse response = mock(ServerResponse.class);
        when(response.status(anyInt())).thenReturn(response);
        return response;
    }

    private static final class ZeroLengthReadableEntity extends ReadableEntityBase {
        private ZeroLengthReadableEntity() {
            super(ignored -> BufferData.empty(), () -> {
            }, 1);
        }

        @Override
        protected <T> T entityAs(GenericType<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReadableEntity copy(Runnable entityProcessedRunnable) {
            entityProcessedRunnable.run();
            return this;
        }
    }
}
