/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.security.cert.Certificate;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.helidon.common.socket.PeerInfo;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.webserver.http.ServerRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelidonContextInjectorTest {

    @Test
    void shouldInjectContextFieldsDeclaredOnSuperclass() {
        ServerRequest request = mockRequest();
        ResourceInfo resourceInfo = new HelidonResourceInfo(
                HelidonResourceInfoTest.SampleService.class.getName(),
                "void doNothing()");
        HelidonContainerRequestContext context = new HelidonContainerRequestContext(request, resourceInfo);
        TestFilter filter = new TestFilter();

        HelidonContextInjector.inject(filter, context);

        assertSame(resourceInfo, filter.resourceInfo());
        assertNotNull(filter.uriInfo());
        assertEquals(8080, filter.servletRequest().getLocalPort());
    }

    private static ServerRequest mockRequest() {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo remotePeer = mock(PeerInfo.class);
        PeerInfo localPeer = mock(PeerInfo.class);

        when(request.remotePeer()).thenReturn(remotePeer);
        when(request.localPeer()).thenReturn(localPeer);
        when(remotePeer.tlsCertificates()).thenReturn(Optional.<Certificate[]>empty());
        when(localPeer.port()).thenReturn(8080);
        when(request.headers()).thenReturn(ServerRequestHeaders.create());
        return request;
    }

    private static class BaseFilter {
        @Context
        private HttpServletRequest servletRequest;

        @Context
        private ResourceInfo resourceInfo;

        @Context
        private UriInfo uriInfo;

        HttpServletRequest servletRequest() {
            return servletRequest;
        }

        ResourceInfo resourceInfo() {
            return resourceInfo;
        }

        UriInfo uriInfo() {
            return uriInfo;
        }
    }

    private static class TestFilter extends BaseFilter {
    }
}
