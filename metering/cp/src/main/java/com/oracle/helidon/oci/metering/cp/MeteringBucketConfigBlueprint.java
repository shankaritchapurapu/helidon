/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Control plane metering bucket configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
interface MeteringBucketConfigBlueprint {
    /**
     * Bucket name.
     *
     * @return bucket name
     */
    @Option.Configured
    String bucketName();

    /**
     * Service name associated with the bucket.
     *
     * @return service name
     */
    @Option.Configured
    String serviceName();

    /**
     * Meter name associated with the bucket.
     *
     * @return meter name
     */
    @Option.Configured
    String meterName();
}
