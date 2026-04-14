/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.json.binding.Json;

/**
 * Robot representation returned by the example service.
 *
 * @param id robot id
 * @param compartmentId compartment id
 * @param displayName robot display name
 * @param lifecycleState lifecycle state
 */
@Json.Entity
record Robot(String id, String compartmentId, String displayName, String lifecycleState) {
}
