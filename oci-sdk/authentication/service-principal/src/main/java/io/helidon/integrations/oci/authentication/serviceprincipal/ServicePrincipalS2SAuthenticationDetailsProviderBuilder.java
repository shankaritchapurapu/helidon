/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider;

/**
 * Service principal S2S authentication details provider builder.
 */
class ServicePrincipalS2SAuthenticationDetailsProviderBuilder
        extends S2SAuthenticationDetailsProvider.S2SAuthenticationDetailsProviderBuilder {

    ServicePrincipalS2SAuthenticationDetailsProviderBuilder region(Region region) {
        this.region = region;
        return this;
    }
}
