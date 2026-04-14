/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.json.binding.Json;

/**
 * Request payload for creating a robot.
 *
 * @param displayName robot display name
 * @param compartmentId compartment identifier
 */
@Json.Entity
record CreateRobotRequest(String displayName, String compartmentId) {
}
