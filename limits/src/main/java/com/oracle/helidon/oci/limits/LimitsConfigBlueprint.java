/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * OCI limits configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
interface LimitsConfigBlueprint {

    /**
     * Default maximum number of asynchronous threads.
     */
    int DEFAULT_MAX_ASYNC_THREADS = 50;

    /**
     * Connection timeout.
     *
     * @return the connection timeout
     */
    @Option.Configured
    @Option.Default("PT10S")
    Duration connectionTimeout();

    /**
     * Read timeout.
     *
     * @return the read timeout
     */
    @Option.Configured
    @Option.Default("PT1M")
    Duration readTimeout();

    /**
     * Maximum number of asynchronous threads.
     *
     * @return the max asynchronous threads
     */
    @Option.Configured
    @Option.DefaultInt(DEFAULT_MAX_ASYNC_THREADS)
    int maxAsyncThreads();

    /**
     * Client target endpoint.
     *
     * @return endpoint
     */
    @Option.Configured
    Optional<String> endpoint();
}
