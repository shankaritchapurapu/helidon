/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow.client;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for {@link com.oracle.pic.workflow.worker.WorkflowEndpointConfiguration}.
 */
@Prototype.Blueprint
@Prototype.Configured
interface EndpointConfigBlueprint {

    @Option.Configured
    @Option.Default("http://localhost:39000")
    String serverEndpoint();

    @Option.Configured
    @Option.Default("30000")
    int connectTimeoutMillis();

    @Option.Configured
    @Option.Default("30000")
    int workerReadTimeoutMillis();

    @Option.Configured
    @Option.Default("30000")
    int pollerReadTimeoutMillis();
}
