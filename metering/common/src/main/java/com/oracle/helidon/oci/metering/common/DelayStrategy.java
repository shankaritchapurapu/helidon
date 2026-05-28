/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.common;

import java.time.Duration;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Retry delay-strategy configuration types.
 */
final class DelayStrategy {

    private DelayStrategy() {
    }

    /**
     * Supported configured OCI delay-strategy types.
     */
    enum Type {
        /**
         * Fixed-time delay strategy.
         */
        FIXED,
        /**
         * Exponential-backoff delay strategy.
         */
        EXPONENTIAL,
        /**
         * Exponential-backoff-with-jitter delay strategy.
         */
        EXPONENTIAL_WITH_JITTER
    }

    /**
     * Configuration for OCI fixed-time delay strategy.
     */
    @Prototype.Blueprint
    @Prototype.Configured
    interface FixedTimeDelayStrategyConfigBlueprint {

        /**
         * Fixed delay between retry attempts.
         *
         * @return fixed delay
         */
        @Option.Configured
        Duration delay();
    }

    /**
     * Configuration for OCI exponential-backoff delay strategy.
     */
    @Prototype.Blueprint
    @Prototype.Configured
    interface ExponentialBackoffDelayStrategyConfigBlueprint {

        /**
         * Maximum delay cap for the exponential backoff strategy.
         *
         * @return maximum delay cap
         */
        @Option.Configured("max-delay")
        Duration maxDelay();
    }

    /**
     * Configuration for OCI exponential-backoff-with-jitter delay strategy.
     */
    @Prototype.Blueprint
    @Prototype.Configured
    interface ExponentialBackoffDelayStrategyWithJitterConfigBlueprint {

        /**
         * Maximum delay cap for the exponential backoff strategy with jitter.
         *
         * @return maximum delay cap
         */
        @Option.Configured("max-delay")
        Duration maxDelay();
    }
}
