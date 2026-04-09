/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.workflow;

import io.helidon.json.binding.Json;

/**
 * Launch request received by the example endpoint and forwarded to WFaaS.
 *
 * @param resourceId OCI resource id to provision
 * @param simulateFailure whether to drive the failing workflow path
 */
@Json.Entity
record LaunchWorkflowRequest(String resourceId,
                             boolean simulateFailure) {
}
