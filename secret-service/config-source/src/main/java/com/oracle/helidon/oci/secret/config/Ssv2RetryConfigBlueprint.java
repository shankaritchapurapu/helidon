/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * SSv2 client retry configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
interface Ssv2RetryConfigBlueprint {
    /**
     * Maximum retry attempts.
     *
     * @return maximum retry attempts
     */
    @Option.Configured
    @Option.DefaultInt(DefaultSsv2Client.DEFAULT_MAX_RETRIES)
    int maxRetries();

    /**
     * Minimum retry delay in milliseconds.
     *
     * @return minimum retry delay
     */
    @Option.Configured
    @Option.DefaultLong(DefaultSsv2Client.DEFAULT_MIN_RETRY_DELAY_MS)
    long minRetryDelayInMs();

    /**
     * Maximum retry delay in milliseconds.
     *
     * @return maximum retry delay
     */
    @Option.Configured
    Optional<Long> maxRetryDelayInMs();
}
