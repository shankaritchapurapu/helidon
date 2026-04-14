/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.json.binding.Json;

/**
 * Request payload for updating a robot.
 *
 * @param displayName new robot display name
 */
@Json.Entity
record UpdateRobotRequest(String displayName) {
}
