/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.json.binding.Json;

/**
 * Response envelope for endpoints that return either a robot payload or an OCI error.
 *
 * @param responseStatus HTTP response status code
 * @param payload robot payload when the call succeeds
 * @param error OCI-style error payload when the call fails
 */
@Json.Entity
record RobotResponse(int responseStatus, Robot payload, OciErrorResponse error) {
}
