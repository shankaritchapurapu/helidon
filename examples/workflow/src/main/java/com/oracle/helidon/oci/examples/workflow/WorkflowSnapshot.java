/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.workflow;

import io.helidon.json.binding.Json;

/**
 * Small workflow snapshot returned by the example service.
 *
 * @param workflowInstanceId workflow instance id
 * @param status workflow status
 * @param tag optional current tag or step
 */
@Json.Entity
record WorkflowSnapshot(String workflowInstanceId, String status, String tag) {
}
