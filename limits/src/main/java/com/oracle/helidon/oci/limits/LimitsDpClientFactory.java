/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.oci.limits.LimitsDPClient;

/**
 * Factory that creates a configured {@link LimitsDPClient} instance.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
final class LimitsDpClientFactory implements Supplier<LimitsDPClient> {

    /**
     * Limits client configuration.
     */
    private final LimitsConfig limitsConfig;

    /**
     * Limits authentication provider factory.
     */
    private final LimitsAuthProviderFactory authProviderFactory;

    /**
     * Create a new factory.
     *
     * @param clientConfig limits client configuration
     * @param authProviderFactory limits authentication provider factory
     */
    @Service.Inject
    LimitsDpClientFactory(
            LimitsConfig clientConfig,
            LimitsAuthProviderFactory authProviderFactory) {
        this.limitsConfig = clientConfig;
        this.authProviderFactory = authProviderFactory;
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
        return builder.build(authProviderFactory.authProvider());
    }
}
