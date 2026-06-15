/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.net.URI;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider.S2SAuthenticationDetailsProviderBuilder;

/**
 * Service principal builder provider, uses the {@link S2SAuthenticationDetailsProviderBuilder}.
 */
@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class ServicePrincipalBuilderProvider implements Supplier<S2SAuthenticationDetailsProviderBuilder> {
    private final OciConfig config;

    ServicePrincipalBuilderProvider(OciConfig config) {
        this.config = config;
    }

    @Override
    public S2SAuthenticationDetailsProviderBuilder get() {
        ServicePrincipalS2SAuthenticationDetailsProviderBuilder builder = getBuilder();

        config.region()
                .ifPresent(builder::region);
        config.federationEndpoint()
                .map(URI::toString)
                .ifPresent(builder::federationEndpoint);
        config.imdsBaseUri()
                .map(URI::toString)
                .ifPresent(builder::metadataBaseUrl);
        config.tenantId()
                .ifPresent(builder::tenancyId);

        return builder.useInstancePrincipals();
    }

    ServicePrincipalS2SAuthenticationDetailsProviderBuilder getBuilder() {
        return new ServicePrincipalS2SAuthenticationDetailsProviderBuilder();
    }
}
