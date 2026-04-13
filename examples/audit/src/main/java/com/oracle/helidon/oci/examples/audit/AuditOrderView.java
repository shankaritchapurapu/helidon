/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.audit;

import java.util.List;

import io.helidon.json.binding.Json;

/**
 * Response for retrieving an order in the audit example.
 *
 * @param orderId order identifier
 * @param status synthetic order status
 * @param expand requested expansion mode
 * @param allowedActions sample actions for the order
 */
@Json.Entity
record AuditOrderView(String orderId,
                      String status,
                      String expand,
                      List<String> allowedActions) {
}
