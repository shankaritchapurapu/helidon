/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow.client;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for {@link com.oracle.pic.workflow.worker.ChastWorkflowClient}.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.workflow")
interface WorkflowClientConfigBlueprint {

    @Option.Configured
    @Option.Default("localhost")
    String domainId();

    @Option.Configured
    @Option.DefaultMethod("create")
    EndpointConfig endpointDetails();
}
