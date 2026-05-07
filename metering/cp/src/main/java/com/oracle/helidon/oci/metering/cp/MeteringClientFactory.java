/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.bling.emit.client.BlingMeteringAuthClient;
import com.oracle.pic.bling.emit.client.MeteringClient;

/**
 * Factory that creates the control plane direct metering client.
 */
@Service.Singleton
class MeteringClientFactory implements Supplier<MeteringClient> {
    private final Config config;
    private final BasicAuthenticationDetailsProvider authProvider;

    @Service.Inject
    MeteringClientFactory(Config config,
                          BasicAuthenticationDetailsProvider authProvider) {
        this.config = config;
        this.authProvider = authProvider;
    }

    @Override
    public MeteringClient get() {
        DirectMeteringConfig directConfig = DirectMeteringConfig.create(config.get("oci.metering.direct"));
        return new BlingMeteringAuthClient(directConfig.endpoint(), directConfig.clientId(), authProvider);
    }
}
