/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for OCI Workflow-as-a-Service integration.
 * <p>
 * This blueprint is mapped from the {@code oci.workflow} configuration subtree
 * and is used to generate {@link WorkflowConfig} during the build.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.workflow")
interface WorkflowConfigBlueprint {

    /**
     * Whether workflow integration is enabled.
     *
     * @return {@code true} when enabled
     */
    @Option.Configured
    @Option.Default("localhost")
    String domainId();

    @Option.Configured
    @Option.DefaultMethod("create")
    EndpointConfig endpointDetails();

    @Option.Configured
    Optional<String> workerIdentifier();

    @Option.Configured
    Optional<String> dynamicSslContextProviderName();

    @Option.Configured
    Optional<RetryPolicyConfig> retryPolicy();
}
