/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;

/**
 * A factory to create an instance of {@link SplatMtlsFeature} based on {@link SplatMtlsConfig}.
 */
@Service.Singleton
record SplatMtlsFeatureFactory(SplatMtlsConfig config)
        implements Supplier<SplatMtlsFeature> {

    /**
     * Get a new instance of {@link SplatMtlsFeature} based on {@link SplatMtlsConfig}..
     *
     * @return a new instance of {@link SplatMtlsFeature}
     */
    @Override
    public SplatMtlsFeature get() {
        return SplatMtlsFeature.create(config);
    }
}
