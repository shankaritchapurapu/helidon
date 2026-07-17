/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Optional;

import io.helidon.common.socket.PeerInfo;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.media.ReadableEntity;
import io.helidon.webserver.http.ServerRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelidonContainerRequestContextTest {

    @Test
    void shouldDetectEntityWithoutContentLength() {
        ServerRequest request = mockRequest();
        ReadableEntity entity = mock(ReadableEntity.class);

        when(entity.hasEntity()).thenReturn(true);
        when(request.content()).thenReturn(entity);

        HelidonContainerRequestContext context = new HelidonContainerRequestContext(
                request,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertTrue(context.hasEntity());
    }

    @Test
    void shouldDetectBodylessRequestFromHelidonEntityState() {
        ServerRequest request = mockRequest();
        ReadableEntity entity = mock(ReadableEntity.class);

        when(entity.hasEntity()).thenReturn(false);
        when(request.content()).thenReturn(entity);

        HelidonContainerRequestContext context = new HelidonContainerRequestContext(
                request,
                new HelidonResourceInfo(HelidonResourceInfoTest.SampleService.class.getName(), "void doNothing()"));

        assertFalse(context.hasEntity());
    }

    @Test
    void shouldExposeTlsCertificatesAsRequestProperty() {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo peerInfo = mock(PeerInfo.class);
        X509Certificate first = mock(X509Certificate.class);
        X509Certificate second = mock(X509Certificate.class);

        when(request.remotePeer()).thenReturn(peerInfo);
        when(peerInfo.tlsCertificates()).thenReturn(Optional.of(new Certificate[] {first, second}));

        HelidonContainerRequestContext context =
                new HelidonContainerRequestContext(request, new HelidonResourceInfo(
                        HelidonResourceInfoTest.SampleService.class.getName(),
                        "void doNothing()"));

        Object certificates = context.getProperty(HelidonContainerRequestContext.X509_CERTIFICATE_PROPERTY);
        assertArrayEquals(new X509Certificate[] {first, second}, (X509Certificate[]) certificates);
    }

    @Test
    void shouldIgnoreNonX509TlsCertificates() {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo peerInfo = mock(PeerInfo.class);
        Certificate certificate = mock(Certificate.class);

        when(request.remotePeer()).thenReturn(peerInfo);
        when(peerInfo.tlsCertificates()).thenReturn(Optional.of(new Certificate[] {certificate}));

        HelidonContainerRequestContext context =
                new HelidonContainerRequestContext(request, new HelidonResourceInfo(
                        HelidonResourceInfoTest.SampleService.class.getName(),
                        "void doNothing()"));

        assertNull(context.getProperty(HelidonContainerRequestContext.X509_CERTIFICATE_PROPERTY));
    }

    private static ServerRequest mockRequest() {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo peerInfo = mock(PeerInfo.class);

        when(request.remotePeer()).thenReturn(peerInfo);
        when(peerInfo.tlsCertificates()).thenReturn(Optional.<Certificate[]>empty());
        when(request.headers()).thenReturn(ServerRequestHeaders.create());
        return request;
    }
}
