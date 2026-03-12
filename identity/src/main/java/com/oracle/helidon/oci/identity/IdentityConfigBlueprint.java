/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for OCI Identity integration.
 * <p>
 * This blueprint groups together the configuration needed for both
 * authentication and authorization when interacting with the OCI Identity
 * service. It is typically used as the root configuration object for
 * identity-related features and is mapped from the {@code oci.identity}
 * configuration subtree.
 * </p>
 *
 * @see AuthenticationConfig
 * @see AuthorizationConfig
 */
@Prototype.Blueprint
@Prototype.Configured("oci.identity")
interface IdentityConfigBlueprint {

    /**
     * Returns the authentication configuration used when interacting with the
     * Oracle Cloud Infrastructure Identity service.
     *
     * @return the {@link AuthenticationConfig} that defines how requests to the
     *         OCI Identity service are authenticated; never {@code null}
     * @see AuthenticationConfig
     */
    @Option.Configured
    AuthenticationConfig authentication();

    /**
     * Returns the authorization configuration used when interacting with the
     * Oracle Cloud Infrastructure Identity service.
     *
     * @return the {@link AuthorizationConfig} that defines how authorization is
     *         performed against the OCI Identity service; never {@code null}
     * @see AuthorizationConfig
     */
    @Option.Configured
    AuthorizationConfig authorization();
}
