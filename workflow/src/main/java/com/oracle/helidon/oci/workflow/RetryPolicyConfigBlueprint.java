/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.time.Duration;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for {@link com.oracle.pic.workflow.worker.RetryPolicy}.
 */
@Prototype.Blueprint
@Prototype.Configured
interface RetryPolicyConfigBlueprint {

    @Option.Configured
    int maxRetryCount();

    @Option.Configured
    Duration delayBetweenRetry();

    @Option.Configured
    double jitterFactor();
}
