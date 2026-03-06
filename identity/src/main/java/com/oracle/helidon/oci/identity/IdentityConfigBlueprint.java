/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for OCI Identity.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.identity")
interface IdentityConfigBlueprint {

    /**
     * Authentication config.
     *
     * @return authentication config.
     */
    @Option.Configured
    AuthenticationConfig authentication();

    /**
     * Authorization config.
     *
     * @return authentication config.
     */
    @Option.Configured
    AuthorizationConfig authorization();
}
