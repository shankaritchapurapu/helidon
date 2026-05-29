/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;

/**
 * Configuration specific to Kiev as a service.
 */
@Prototype.Blueprint
@Prototype.Configured
interface KievServiceConfigBlueprint {

    /**
     * Compartment containing the Kiev store.
     *
     * @return compartment id
     */
    @Option.Configured
    String compartmentId();

    /**
     * Frontend endpoint for the Kiev service.
     *
     * @return frontend endpoint
     */
    @Option.Configured
    String frontendEndpoint();

    /**
     * Store locality.
     *
     * @return optional client registry locality
     */
    @Option.Configured
    Optional<ClientRegistryLocality> locality();

    /**
     * Authentication settings for the service client.
     *
     * @return optional auth configuration
     */
    @Option.Configured
    Optional<KievServiceAuthConfig> auth();
}
