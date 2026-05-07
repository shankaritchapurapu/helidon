/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Direct control plane metering configuration mapped from {@code oci.metering}.
 */
@Prototype.Blueprint
@Prototype.Configured
interface DirectMeteringConfigBlueprint {
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
