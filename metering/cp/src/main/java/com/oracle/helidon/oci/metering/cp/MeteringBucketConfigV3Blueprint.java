/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.Set;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Version 3 control plane metering bucket configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
interface MeteringBucketConfigV3Blueprint {
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
     * Meter names associated with the bucket.
     *
     * @return meter names
     */
    @Option.Configured
    Set<String> meterNames();
}
