/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common;

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
    @Prototype.CustomMethods(ConfigSupport.DelayStrategySupport.class)
    interface FixedTimeDelayStrategyConfigBlueprint extends Prototype.Factory<com.oracle.bmc.waiter.DelayStrategy> {

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
    @Prototype.CustomMethods(ConfigSupport.DelayStrategySupport.class)
    interface ExponentialBackoffDelayStrategyConfigBlueprint
            extends Prototype.Factory<com.oracle.bmc.waiter.DelayStrategy> {

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
    @Prototype.CustomMethods(ConfigSupport.DelayStrategySupport.class)
    interface ExponentialBackoffDelayStrategyWithJitterConfigBlueprint
            extends Prototype.Factory<com.oracle.bmc.waiter.DelayStrategy> {

        /**
         * Maximum delay cap for the exponential backoff strategy with jitter.
         *
         * @return maximum delay cap
         */
        @Option.Configured("max-delay")
        Duration maxDelay();
    }
}
