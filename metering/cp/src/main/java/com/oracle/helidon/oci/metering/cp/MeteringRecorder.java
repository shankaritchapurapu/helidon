/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import io.helidon.config.NamedService;
import io.helidon.service.registry.Service;

/**
 * Narrow control plane metering abstraction used by generated interceptors and application code.
 */
@Service.Contract
public interface MeteringRecorder extends NamedService {
    /**
     * Record a bounded metering event.
     *
     * @param event event to record
     * @throws Exception when recording fails
     */
    void record(MeteringEvent event) throws Exception;

    /**
     * Unwrap this recorder or its native delegate.
     *
     * @param type requested type
     * @param <T> requested type
     * @return unwrapped value
     * @throws IllegalArgumentException when neither this recorder nor its delegate matches the requested type
     */
    default <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new IllegalArgumentException("Metering recorder cannot unwrap " + type.getName());
    }
}
