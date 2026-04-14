/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.json.binding.Json;

/**
 * Collection item returned by list operations.
 *
 * @param id robot id
 * @param compartmentId compartment id
 * @param displayName display name
 * @param lifecycleState lifecycle state
 */
@Json.Entity
record RobotSummary(String id, String compartmentId, String displayName, String lifecycleState) {
}
