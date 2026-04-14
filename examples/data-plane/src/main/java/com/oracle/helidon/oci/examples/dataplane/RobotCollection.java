/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import java.util.List;

import io.helidon.json.binding.Json;

/**
 * Robot collection response.
 *
 * @param items returned items
 * @param count number of returned items
 */
@Json.Entity
record RobotCollection(List<RobotSummary> items, int count) {
}
