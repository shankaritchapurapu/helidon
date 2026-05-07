/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.bling.emit.client.BlingMeteringAuthClient;

/**
 * Configured provider for direct control plane metering.
 */
@Service.Singleton
class DirectMeteringRecorderProvider implements MeteringRecorderProvider {
    private final BasicAuthenticationDetailsProvider authProvider;

    @Service.Inject
    DirectMeteringRecorderProvider(BasicAuthenticationDetailsProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public String configKey() {
        return "direct";
    }

    @Override
    public MeteringRecorder create(Config config, String name) {
        DirectMeteringConfig directConfig = DirectMeteringConfig.create(config);
        return new DirectMeteringRecorder(name,
                                          new BlingMeteringAuthClient(directConfig.endpoint(),
                                                                      directConfig.clientId(),
                                                                      authProvider));
    }
}
