/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpFeatures;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.oracle.pic.sherlock.collector.AuditLogger;
import com.oracle.pic.sherlock.common.event.AuditEventV2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RoutingTest
class AuditV2SplatFlagDisabledTest {
    private static final AtomicReference<AuditLogger> AUDIT_LOGGER_REF = new AtomicReference<>();
    private static final AuditLogger DELEGATING_LOGGER = event -> AUDIT_LOGGER_REF.get().log(event);
    private static final HeaderName SPLAT_AUDITED_HEADER = HeaderNames.create("oci-splat-audited");
    private static final String SPLAT_REQUEST_VALIDATED_CONTEXT_KEY =
            "com.oracle.helidon.oci.splat.requestValidated";

    private final DirectClient client;
    private AuditLogger auditLogger;

    AuditV2SplatFlagDisabledTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        router.get("/trusted-splat", (req, res) -> {
            req.context().register(SPLAT_REQUEST_VALIDATED_CONTEXT_KEY, Boolean.TRUE);
            res.status(Status.OK_200).send();
        });
    }

    @SetUpFeatures
    static List<ServerFeature> features() {
        AuditV2Config config = AuditV2Config.builder()
                .enabled(true)
                .respectSplatAuditedFlag(false)
                .eventSource("test-source")
                .tenantId("test-tenant")
                .compartmentId("test-compartment")
                .resourceId("test-resource")
                .buildPrototype();
        return List.of(AuditV2Feature.create(config, DELEGATING_LOGGER));
    }

    @BeforeEach
    void setUp() {
        auditLogger = Mockito.mock(AuditLogger.class);
        AUDIT_LOGGER_REF.set(auditLogger);
    }

    @Test
    void testDisabledFlagRespectAlwaysAuditsValidatedSplatRequest() throws Exception {
        // Disabling flag support must make even a trusted SPLAT audited header ineffective.
        var captor = ArgumentCaptor.forClass(AuditEventV2.class);

        try (Http1ClientResponse response = client.get("/trusted-splat")
                .header(SPLAT_AUDITED_HEADER, "true")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
        }

        Mockito.verify(auditLogger, Mockito.timeout(2000).atLeastOnce()).log(captor.capture());
        assertThat(captor.getAllValues()
                           .stream()
                           .anyMatch(event -> "/trusted-splat".equals(event.getData().getRequest().getPath())),
                   is(true));
    }
}
