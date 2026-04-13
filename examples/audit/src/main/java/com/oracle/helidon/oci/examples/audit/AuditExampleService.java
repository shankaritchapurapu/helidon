/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.audit;

import java.util.List;
import java.util.Optional;

import io.helidon.service.registry.Service;

/**
 * Small application service used by the audit example endpoint.
 */
@Service.Singleton
class AuditExampleService {
    AuditOrderView getOrder(String orderId, Optional<String> expand) {
        String requestedExpansion = expand.orElse("summary");
        return new AuditOrderView(orderId,
                                  "READY_FOR_APPROVAL",
                                  requestedExpansion,
                                  List.of("approve", "reject"));
    }

    AuditApprovalResult approveOrder(String orderId, String approvedBy) {
        return new AuditApprovalResult(orderId,
                                       approvedBy,
                                       "APPROVED",
                                       "Use the oci-splat-audit-verify header to inspect audit execution.");
    }
}
