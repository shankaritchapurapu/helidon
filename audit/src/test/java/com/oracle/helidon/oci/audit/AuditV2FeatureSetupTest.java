/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.Set;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;

import org.junit.jupiter.api.Test;

import com.oracle.pic.sherlock.collector.AuditLogger;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditV2FeatureSetupTest {

    @Test
    void setupRegistersAuditFilterOnDefaultAndNamedSockets() {
        AuditV2Config config = AuditV2Config.builder()
                .enabled(true)
                .eventSource("test-source")
                .buildPrototype();
        ServerFeature.ServerFeatureContext featureContext = mock(ServerFeature.ServerFeatureContext.class);
        ServerFeature.SocketBuilders defaultSocket = mock(ServerFeature.SocketBuilders.class);
        ServerFeature.SocketBuilders adminSocket = mock(ServerFeature.SocketBuilders.class);
        ServerFeature.SocketBuilders privateSocket = mock(ServerFeature.SocketBuilders.class);
        HttpRouting.Builder defaultRouting = mock(HttpRouting.Builder.class);
        HttpRouting.Builder adminRouting = mock(HttpRouting.Builder.class);
        HttpRouting.Builder privateRouting = mock(HttpRouting.Builder.class);

        when(featureContext.sockets()).thenReturn(Set.of("admin", "private"));
        when(featureContext.socket(WebServer.DEFAULT_SOCKET_NAME)).thenReturn(defaultSocket);
        when(featureContext.socket("admin")).thenReturn(adminSocket);
        when(featureContext.socket("private")).thenReturn(privateSocket);
        when(defaultSocket.httpRouting()).thenReturn(defaultRouting);
        when(adminSocket.httpRouting()).thenReturn(adminRouting);
        when(privateSocket.httpRouting()).thenReturn(privateRouting);

        AuditV2Feature.create(config, mock(AuditLogger.class)).setup(featureContext);

        verify(defaultRouting).addFilter(isA(AuditV2Filter.class));
        verify(adminRouting).addFilter(isA(AuditV2Filter.class));
        verify(privateRouting).addFilter(isA(AuditV2Filter.class));
    }

    @Test
    void setupDoesNotRegisterAuditFilterWhenDisabled() {
        AuditV2Config config = AuditV2Config.builder()
                .enabled(false)
                .eventSource("test-source")
                .buildPrototype();
        ServerFeature.ServerFeatureContext featureContext = mock(ServerFeature.ServerFeatureContext.class);

        AuditV2Feature.create(config, mock(AuditLogger.class)).setup(featureContext);

        verify(featureContext, never()).socket(WebServer.DEFAULT_SOCKET_NAME);
    }
}
