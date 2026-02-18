/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.oci.limits.LimitsDPClient;
import com.oracle.oci.limits.config.LimitsDPClientConfiguration;

/**
 * Factory that creates a configured {@link LimitsDPClient} instance.
 */
@Service.Singleton
class LimitsDpClientFactory implements Supplier<LimitsDPClient> {

    /**
     * Limits client configuration.
     */
    private final LimitsConfig limitsConfig;

    /**
     * OCI authentication details provider.
     */
    private final AbstractAuthenticationDetailsProvider
            authenticationDetailsProvider;

    /**
     * Create a new factory.
     *
     * @param clientConfig limits client configuration
     * @param authProvider OCI authentication details provider
     */
    @Service.Inject
    LimitsDpClientFactory(
            LimitsConfig clientConfig,
            AbstractAuthenticationDetailsProvider authProvider) {
        this.limitsConfig = clientConfig;
        this.authenticationDetailsProvider = authProvider;
    }

    /**
     * Create a new {@link LimitsDPClient} instance.
     *
     * @return limits data plane client
     */
    @Override
    public LimitsDPClient get() {
        LimitsDPClient.LimitsClientBuilder builder = LimitsDPClient.builder();
        LimitsDPClientConfiguration clientConfiguration =
                LimitsDPClientConfiguration.builder()
                        .maxAsyncThreads(limitsConfig.maxAsyncThreads())
                        .connectionTimeoutMillis(
                                (int) limitsConfig.connectionTimeout()
                                        .toMillis())
                        .readTimeoutMillis(
                                (int) limitsConfig.readTimeout().toMillis())
                        .build();

        builder.authenticationDetailsProvider(authenticationDetailsProvider)
                .clientConfiguration(clientConfiguration);
        return builder.build();
    }
}
