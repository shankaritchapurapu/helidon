/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.audit;

import java.util.List;
import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import com.oracle.pic.sherlock.collector.AuditRIO;
import com.oracle.pic.sherlock.collector.OperationSynchronousType;

/**
 * HTTP endpoint that demonstrates audit configuration through a small JSON API.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@RestServer.Header(name = "x-audit-example-version", value = "v1")
@Http.Path("/audit")
@Service.Singleton
class AuditEndpoint {
    static final String EXAMPLE_COMPARTMENT_ID = "ocid1.compartment.oc1..aaaaaaaahelidonauditexample";
    static final String EXAMPLE_TENANT_ID = "ocid1.tenancy.oc1..aaaaaaaahelidonauditexample";

    private final AuditExampleService auditService;

    @Service.Inject
    AuditEndpoint(AuditExampleService auditService) {
        this.auditService = auditService;
    }

    @Http.GET
    @Http.Path("/orders")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    AuditOrderView get(@Http.QueryParam("orderId") Optional<String> orderId,
                       @Http.QueryParam("expand") Optional<String> expand,
                       AuditPayloadAppender audit) {
        String id = required(orderId.orElse(null), "orderId");
        enrichAudit(audit, "GetAuditOrder", "audit-order-" + id);
        return auditService.getOrder(id, normalized(expand));
    }

    @Http.POST
    @Http.Path("/orders/approve")
    @Http.Consumes(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    AuditApprovalResult approve(@Http.QueryParam("orderId") Optional<String> orderId,
                                @Http.Entity String approver,
                                AuditPayloadAppender audit) {
        String id = required(orderId.orElse(null), "orderId");
        enrichAudit(audit, "ApproveAuditOrder", "audit-order-" + id);
        return auditService.approveOrder(id,
                                         required(approver, "approver"));
    }

    private static void enrichAudit(AuditPayloadAppender audit, String eventName, String resourceId) {
        audit.setEventName(eventName, OperationSynchronousType.None);
        audit.overridePrincipalTenantId(EXAMPLE_TENANT_ID);
        audit.overrideCompartmentId(EXAMPLE_COMPARTMENT_ID);
        audit.setResourceId(resourceId);
        audit.setResourceName(resourceId);
        audit.appendToAuditRios(List.of(new AuditRIO(EXAMPLE_COMPARTMENT_ID, resourceId)));
    }

    private static String required(String value, String key) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new HttpException("Request must contain " + key, Status.BAD_REQUEST_400);
        }
        return trimmed;
    }

    private static Optional<String> normalized(Optional<String> value) {
        return value.map(String::trim)
                .filter(trimmed -> !trimmed.isEmpty());
    }
}
