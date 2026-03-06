/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for Authorization.
 */
@Prototype.Blueprint
@Prototype.Configured
interface AuthorizationConfigBlueprint {

    /**
     * Whether the authentication is enabled.
     *
     * @return {@code true} if enabled; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();
}
