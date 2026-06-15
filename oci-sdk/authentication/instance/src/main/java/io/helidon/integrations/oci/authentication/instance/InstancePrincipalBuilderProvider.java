/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.instance;

import java.net.URI;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider.InstancePrincipalsAuthenticationDetailsProviderBuilder;

/**
 * Instance principal builder provider, uses the
 * {@link InstancePrincipalsAuthenticationDetailsProviderBuilder}.
 */
@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class InstancePrincipalBuilderProvider implements Supplier<InstancePrincipalsAuthenticationDetailsProviderBuilder> {
    private final OciConfig config;

    InstancePrincipalBuilderProvider(OciConfig config) {
        this.config = config;
    }

    @Override
    public InstancePrincipalsAuthenticationDetailsProviderBuilder get() {
        var builder = getBuilder();

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

    InstancePrincipalsAuthenticationDetailsProviderBuilder getBuilder() {
        return InstancePrincipalsAuthenticationDetailsProvider.builder();
    }

}
