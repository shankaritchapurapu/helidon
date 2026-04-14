/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.json.binding.Json;

/**
 * Status payload returned by the example root endpoint.
 *
 * @param serviceName service name
 * @param requestId current request id
 * @param auditEnabled whether audit is enabled
 * @param robotCount number of robots currently stored
 */
@Json.Entity
record DataPlaneStatus(String serviceName,
                       String requestId,
                       boolean auditEnabled,
                       int robotCount) {
}
