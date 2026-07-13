/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.util.Optional;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

/**
 * Provides OCI service-principal authentication details when they can be
 * created in the current runtime environment.
 */
@FunctionalInterface
public interface OciServicePrincipalProvider {

    /**
     * Returns the available service-principal authentication details.
     *
     * @return service-principal authentication details, or empty when the
     *         current environment cannot provide a service principal
     */
    Optional<BasicAuthenticationDetailsProvider> provider();
}
