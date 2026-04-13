/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.audit;

import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

/**
 * HTTP endpoint that demonstrates audit configuration through a small JSON API.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@RestServer.Header(name = "x-audit-example-version", value = "v1")
@Http.Path("/audit")
@Service.Singleton
class AuditEndpoint {
    private final AuditExampleService auditService;

    @Service.Inject
    AuditEndpoint(AuditExampleService auditService) {
        this.auditService = auditService;
    }

    @Http.GET
    @Http.Path("/orders")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    AuditOrderView get(@Http.QueryParam("orderId") Optional<String> orderId,
                       @Http.QueryParam("expand") Optional<String> expand) {
        return auditService.getOrder(required(orderId.orElse(null), "orderId"),
                                     normalized(expand));
    }

    @Http.POST
    @Http.Path("/orders/approve")
    @Http.Consumes(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    AuditApprovalResult approve(@Http.QueryParam("orderId") Optional<String> orderId,
                                @Http.Entity String approver) {
        return auditService.approveOrder(required(orderId.orElse(null), "orderId"),
                                         required(approver, "approver"));
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
