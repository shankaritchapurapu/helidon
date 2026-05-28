/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.common;

import java.time.Duration;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Grouping type for retry termination-strategy configuration blueprints.
 */
final class TerminationStrategy {

    private TerminationStrategy() {
    }

    /**
     * Supported configured OCI termination-strategy types.
     */
    enum Type {
        /**
         * Maximum-attempts termination strategy.
         */
        MAX_ATTEMPTS,
        /**
         * Maximum-time termination strategy.
         */
        MAX_TIME
    }

    /**
     * Configuration for OCI max-attempts termination strategy.
     */
    @Prototype.Blueprint
    @Prototype.Configured
    interface MaxAttemptsTerminationStrategyConfigBlueprint {

        /**
         * Maximum retry attempts before terminating.
         *
         * @return maximum retry attempts
         */
        @Option.Configured("max-attempts")
        int maxAttempts();
    }

    /**
     * Configuration for OCI max-time termination strategy.
     */
    @Prototype.Blueprint
    @Prototype.Configured
    interface MaxTimeTerminationStrategyConfigBlueprint {

        /**
         * Maximum retry time before terminating.
         *
         * @return maximum retry time
         */
        @Option.Configured("max-time")
        Duration maxTime();
    }
}
