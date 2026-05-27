/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * SSv2 client TLS configuration.
 */
@Prototype.Blueprint
@Prototype.Configured
interface Ssv2TlsConfigBlueprint {
    /**
     * Path to the CA bundle used by the SSv2 client.
     *
     * @return CA bundle path
     */
    @Option.Configured
    @Option.Default(DefaultSsv2Client.DEFAULT_CA_BUNDLE)
    String caBundle();
}
