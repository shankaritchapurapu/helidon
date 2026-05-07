/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Control plane metering root configuration mapped from {@code oci}.
 */
@Prototype.Blueprint
@Prototype.Configured("oci")
@Prototype.RegistrySupport
interface MeteringConfigBlueprint {
    /**
     * Selected metering recorder implementation.
     *
     * @return selected metering recorder
     */
    @Option.Configured
    @Option.Provider(value = MeteringRecorderProvider.class, discoverServices = false)
    Optional<MeteringRecorder> metering();
}
