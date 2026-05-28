/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.pic.bling.clients.BlingPublisherClient;

/**
 * Configuration used to create the native Bling publisher client.
 */
@Prototype.Blueprint
@Prototype.Configured
@Prototype.CustomMethods(ConfigSupport.BlingPublisherClientSupport.class)
interface BlingPublisherClientConfigBlueprint extends Prototype.Factory<BlingPublisherClient> {
    /**
     * Bling ingest endpoint.
     *
     * @return endpoint
     */
    @Option.Configured
    String endpoint();

    /**
     * Metering client ID.
     *
     * @return client ID
     */
    @Option.Configured
    String clientId();
}
