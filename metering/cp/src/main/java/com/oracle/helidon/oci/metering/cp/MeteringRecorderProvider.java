/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import io.helidon.config.ConfiguredProvider;
import io.helidon.service.registry.Service;

/**
 * Configured provider for control plane metering recorder implementations.
 */
@Service.Contract
public interface MeteringRecorderProvider extends ConfiguredProvider<MeteringRecorder> {
}
