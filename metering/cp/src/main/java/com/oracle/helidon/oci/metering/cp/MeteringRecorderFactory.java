/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.service.registry.Service;

/**
 * Factory that creates the selected control plane metering recorder.
 */
@Service.Singleton
class MeteringRecorderFactory implements Supplier<MeteringRecorder> {
    private final Config config;

    @Service.Inject
    MeteringRecorderFactory(Config config) {
        this.config = config;
    }

    @Override
    public MeteringRecorder get() {
        return MeteringConfig.create(config.get("oci"))
                .metering()
                .orElseThrow(() -> new ConfigException("Missing required config value: oci.metering"));
    }
}
