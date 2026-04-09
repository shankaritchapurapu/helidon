/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.time.Duration;

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
    @Option.Default("PT30S")
    Duration connectTimeout();

    @Option.Configured
    @Option.Default("PT30S")
    Duration workerReadTimeout();

    @Option.Configured
    @Option.Default("PT30S")
    Duration pollerReadTimeout();
}
