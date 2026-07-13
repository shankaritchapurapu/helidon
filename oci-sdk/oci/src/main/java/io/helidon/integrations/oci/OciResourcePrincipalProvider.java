/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.util.Optional;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

/**
 * Provides OCI resource-principal authentication details when they are
 * available in the current runtime environment.
 */
@FunctionalInterface
public interface OciResourcePrincipalProvider {

    /**
     * Returns the available resource-principal authentication details.
     *
     * @return resource-principal authentication details, or empty when the
     *         current environment does not provide a resource principal
     */
    Optional<BasicAuthenticationDetailsProvider> provider();
}
