/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.audit;

import io.helidon.json.binding.Json;

/**
 * Response for approving an order in the audit example.
 *
 * @param orderId order identifier
 * @param approvedBy approver name
 * @param status synthetic approval status
 * @param auditHint hint about the audit verification header
 */
@Json.Entity
record AuditApprovalResult(String orderId,
                           String approvedBy,
                           String status,
                           String auditHint) {
}
