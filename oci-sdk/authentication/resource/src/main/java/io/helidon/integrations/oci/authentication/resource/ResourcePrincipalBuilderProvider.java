/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.resource;

import java.net.URI;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder;

/**
 * Resource principal builder provider, uses the
 * {@link ResourcePrincipalAuthenticationDetailsProviderBuilder}.
 */
@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class ResourcePrincipalBuilderProvider implements Supplier<ResourcePrincipalAuthenticationDetailsProviderBuilder> {
    private final OciConfig config;

    ResourcePrincipalBuilderProvider(OciConfig config) {
        this.config = config;
    }

    @Override
    public ResourcePrincipalAuthenticationDetailsProviderBuilder get() {
        var builder = ResourcePrincipalAuthenticationDetailsProvider.builder();

        config.federationEndpoint()
                .map(URI::toString)
                .ifPresent(builder::federationEndpoint);
        config.imdsBaseUri()
                .map(URI::toString)
                .ifPresent(builder::metadataBaseUrl);
        config.tenantId()
                .ifPresent(builder::tenancyId);

        return builder;
    }

}
