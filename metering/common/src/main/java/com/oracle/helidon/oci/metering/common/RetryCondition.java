/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.common;

import io.helidon.builder.api.Prototype;

/**
 * Grouping type for retry-condition configuration blueprints.
 */
final class RetryCondition {

    private RetryCondition() {
    }

    /**
     * Supported configured OCI retry-condition types.
     */
    enum Type {
        /**
         * Default retry condition.
         */
        DEFAULT,
        /**
         * Retry condition that also retries on an open circuit breaker.
         */
        RETRY_ON_OPEN_CIRCUIT_BREAKER
    }

    /**
     * Configuration for OCI default retry condition.
     */
    @Prototype.Blueprint
    @Prototype.Configured
    interface DefaultRetryConditionConfigBlueprint {
    }

    /**
     * Configuration for OCI retry-on-open-circuit-breaker default retry condition.
     */
    @Prototype.Blueprint
    @Prototype.Configured
    interface RetryOnOpenCircuitBreakerDefaultRetryConditionConfigBlueprint {
    }
}
