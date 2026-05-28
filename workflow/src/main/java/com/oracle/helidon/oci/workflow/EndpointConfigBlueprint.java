/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for {@link com.oracle.pic.workflow.worker.WorkflowEndpointConfiguration}.
 */
@Prototype.Blueprint(decorator = EndpointConfigSupport.class)
@Prototype.Configured
interface EndpointConfigBlueprint {

    @Option.Configured
    @Option.Default("http://localhost:39000")
    String serverEndpoint();

    @Option.Configured
    @Option.Default("PT30S")
    Duration connectTimeout();

    /**
     * Alias for {@link #connectTimeout()} using the common OCI client config name.
     *
     * @return optional connection timeout alias
     */
    @Option.Configured
    Optional<Duration> connectionTimeout();

    @Option.Configured
    @Option.Default("PT30S")
    Duration workerReadTimeout();

    @Option.Configured
    @Option.Default("PT30S")
    Duration pollerReadTimeout();
}
