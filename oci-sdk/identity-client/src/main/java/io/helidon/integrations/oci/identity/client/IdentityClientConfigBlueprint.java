/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.identity.client;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * OCI Java SDK Identity client configuration.
 */
@Prototype.Blueprint
@Prototype.Configured(IdentityClientConfigFactory.OCI_IDENTITY_CLIENT)
interface IdentityClientConfigBlueprint {

    /**
     * Default maximum number of asynchronous threads.
     */
    int DEFAULT_MAX_ASYNC_THREADS = 50;

    /**
     * Connection timeout.
     *
     * @return connection timeout
     */
    @Option.Configured
    @Option.Default("PT10S")
    Duration connectionTimeout();

    /**
     * Read timeout.
     *
     * @return read timeout
     */
    @Option.Configured
    @Option.Default("PT1M")
    Duration readTimeout();

    /**
     * Maximum number of asynchronous threads for OCI SDK asynchronous helpers and waiters.
     * Synchronous Identity calls execute on the calling thread.
     *
     * @return max asynchronous helper threads
     */
    @Option.Configured
    @Option.DefaultInt(DEFAULT_MAX_ASYNC_THREADS)
    int maxAsyncThreads();

    /**
     * Explicit Identity endpoint. When configured, this takes precedence over {@link #region()} and
     * {@link #realmSpecificEndpointTemplateEnabled()}.
     *
     * @return endpoint
     */
    @Option.Configured
    Optional<String> endpoint();

    /**
     * OCI region used to derive the Identity endpoint when no explicit endpoint is configured.
     *
     * @return region
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Whether to use realm-specific endpoint templates. This setting applies only when no explicit
     * {@link #endpoint()} is configured.
     *
     * @return whether realm-specific endpoint templates are enabled
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean realmSpecificEndpointTemplateEnabled();
}
