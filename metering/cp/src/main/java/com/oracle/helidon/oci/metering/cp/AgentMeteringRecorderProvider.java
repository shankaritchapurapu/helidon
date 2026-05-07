/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.pic.bling.emit.MeteringLogStores;

/**
 * Configured provider for agent control plane metering.
 */
@Service.Singleton
class AgentMeteringRecorderProvider implements MeteringRecorderProvider {
    private final Supplier<MeteringLogStores> logStores;

    @Service.Inject
    AgentMeteringRecorderProvider(Supplier<MeteringLogStores> logStores) {
        this.logStores = logStores;
    }

    @Override
    public String configKey() {
        return "agent";
    }

    @Override
    public MeteringRecorder create(Config config, String name) {
        AgentMeteringConfig.create(config);
        return new AgentMeteringRecorder(name, logStores.get());
    }
}
