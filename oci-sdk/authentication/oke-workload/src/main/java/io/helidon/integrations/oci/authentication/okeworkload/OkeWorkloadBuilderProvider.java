/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.okeworkload;

import java.net.URI;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder;

/**
 * OKE Workload Identity builder provider, uses the
 * {@link OkeWorkloadIdentityAuthenticationDetailsProviderBuilder}.
 */
@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class OkeWorkloadBuilderProvider implements Supplier<OkeWorkloadIdentityAuthenticationDetailsProviderBuilder> {
    private final OciConfig config;

    OkeWorkloadBuilderProvider(OciConfig config) {
        this.config = config;
    }

    @Override
    public OkeWorkloadIdentityAuthenticationDetailsProviderBuilder get() {
        var builder = OkeWorkloadIdentityAuthenticationDetailsProvider.builder();

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
