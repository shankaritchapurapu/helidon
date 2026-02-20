/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ClientConfiguration.ClientConfigurationBuilder;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.oci.limits.LimitsDPClient;

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
    private final BasicAuthenticationDetailsProvider authProvider;

    /**
     * Create a new factory.
     *
     * @param clientConfig limits client configuration
     * @param authProvider OCI authentication details provider
     */
    @Service.Inject
    LimitsDpClientFactory(
            LimitsConfig clientConfig,
            BasicAuthenticationDetailsProvider authProvider) {
        this.limitsConfig = clientConfig;
        this.authProvider = authProvider;
    }

    /**
     * Create a new {@link LimitsDPClient} instance.
     *
     * @return limits data plane client
     */
    @Override
    public LimitsDPClient get() {
        ClientConfigurationBuilder builder = ClientConfiguration.builder();
        ClientConfiguration config = builder.maxAsyncThreads(limitsConfig.maxAsyncThreads())
            .connectionTimeoutMillis((int) limitsConfig.connectionTimeout().toMillis())
            .readTimeoutMillis((int) limitsConfig.readTimeout().toMillis())
            .build();
        LimitsDPClient client = new LimitsDPClient(authProvider, config);
        return client;
    }
}
