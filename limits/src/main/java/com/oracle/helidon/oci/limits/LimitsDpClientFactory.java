/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.oci.limits.LimitsDPClient;

/**
 * Factory that creates a configured {@link LimitsDPClient} instance.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
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
        ClientConfiguration config = limitsConfig.client()
                .orElseGet(() -> ClientConfiguration.builder().build());
        var builder = LimitsDPClient.builder().configuration(config);
        limitsConfig.endpoint().ifPresent(builder::endpoint);
        return builder.build(authProvider);
    }
}
