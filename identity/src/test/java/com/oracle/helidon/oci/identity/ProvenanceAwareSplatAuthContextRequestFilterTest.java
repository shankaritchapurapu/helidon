/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.container.ResourceInfo;

import io.helidon.common.context.Context;
import io.helidon.common.socket.PeerInfo;
import io.helidon.common.uri.UriInfo;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.webserver.http.ServerRequest;
import org.junit.jupiter.api.Test;

import com.oracle.helidon.oci.jaxrs.HelidonContainerRequestContext;
import com.oracle.helidon.oci.jaxrs.HelidonContextInjector;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.config.SplatAwareAuthConfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProvenanceAwareSplatAuthContextRequestFilterTest {
    private static final int SPLAT_PORT = 8443;
    private static final String SPLAT_CLIENT_CN = "splat-api-client.us-ashburn-1.oci.oracleiaas.com";
    private static final String SPLAT_REQUEST_VALIDATED_CONTEXT_KEY =
            "com.oracle.helidon.oci.splat.requestValidated";

    @Test
    void shouldMarkProvenanceAfterPortAndCertificateValidation() {
        ServerRequest request = mockRequest(Optional.of(certificate(SPLAT_CLIENT_CN)));
        ProvenanceAwareSplatAuthContextRequestFilter filter = filter(true);
        HelidonContainerRequestContext requestContext = requestContext(request);
        HelidonContextInjector.inject(filter, requestContext);

        assertTrue(filter.isSplatRequest(requestContext));
        assertTrue(isSplatRequestValidated(request));
    }

    @Test
    void shouldNotMarkProvenanceWhenCertificateValidationIsDisabled() {
        ServerRequest request = mockRequest(Optional.empty());
        ProvenanceAwareSplatAuthContextRequestFilter filter = filter(false);
        HelidonContainerRequestContext requestContext = requestContext(request);
        HelidonContextInjector.inject(filter, requestContext);

        assertTrue(filter.isSplatRequest(requestContext));
        assertFalse(isSplatRequestValidated(request));
    }

    @Test
    void shouldNotMarkProvenanceForUnrecognizedClientCertificate() {
        ServerRequest request = mockRequest(Optional.of(certificate("attacker.example")));
        ProvenanceAwareSplatAuthContextRequestFilter filter = filter(true);
        HelidonContainerRequestContext requestContext = requestContext(request);
        HelidonContextInjector.inject(filter, requestContext);

        assertFalse(filter.isSplatRequest(requestContext));
        assertFalse(isSplatRequestValidated(request));
    }

    @Test
    void shouldNotMarkProvenanceOnUnconfiguredPort() {
        // A valid SPLAT certificate is insufficient when the request arrives on a non-SPLAT port.
        ServerRequest request = mockRequest(Optional.of(certificate(SPLAT_CLIENT_CN)), SPLAT_PORT + 1);
        ProvenanceAwareSplatAuthContextRequestFilter filter = filter(true);
        HelidonContainerRequestContext requestContext = requestContext(request);
        HelidonContextInjector.inject(filter, requestContext);

        assertFalse(filter.isSplatRequest(requestContext));
        assertFalse(isSplatRequestValidated(request));
    }

    private static ProvenanceAwareSplatAuthContextRequestFilter filter(boolean validateSplatCert) {
        SplatAwareAuthConfig config = new SplatAwareAuthConfig();
        config.setSplatRequestPort(SPLAT_PORT);
        config.setValidateSplatCert(validateSplatCert);
        return new ProvenanceAwareSplatAuthContextRequestFilter(
                null,
                mock(IAuthorizationClient.class),
                config,
                Region.fromPublicRegionName("us-ashburn-1"));
    }

    private static HelidonContainerRequestContext requestContext(ServerRequest request) {
        return new HelidonContainerRequestContext(request, mock(ResourceInfo.class));
    }

    private static ServerRequest mockRequest(Optional<X509Certificate> certificate) {
        return mockRequest(certificate, SPLAT_PORT);
    }

    private static ServerRequest mockRequest(Optional<X509Certificate> certificate, int localPort) {
        ServerRequest request = mock(ServerRequest.class);
        PeerInfo localPeer = mock(PeerInfo.class);
        PeerInfo remotePeer = mock(PeerInfo.class);

        when(localPeer.port()).thenReturn(localPort);
        when(remotePeer.tlsCertificates()).thenReturn(
                certificate.map(value -> new Certificate[] {value}));
        when(request.localPeer()).thenReturn(localPeer);
        when(request.remotePeer()).thenReturn(remotePeer);
        when(request.context()).thenReturn(Context.create());
        when(request.headers()).thenReturn(ServerRequestHeaders.create());
        when(request.requestedUri()).thenReturn(UriInfo.builder()
                                                    .scheme("https")
                                                    .host("localhost")
                                                    .port(localPort)
                                                    .path("/identity")
                                                    .buildPrototype());
        return request;
    }

    private static X509Certificate certificate(String commonName) {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=" + commonName));
        return certificate;
    }

    private static boolean isSplatRequestValidated(ServerRequest request) {
        return request.context().get(SPLAT_REQUEST_VALIDATED_CONTEXT_KEY, Boolean.class).orElse(false);
    }
}
