/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

/**
 * A factory to create an instance of {@link AuditV2Feature} based on {@link AuditConfig}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
record AuditV2FeatureFactory(AuditV2Config config) implements Supplier<AuditV2Feature> {

    /**
     * Get a new instance of {@link AuditV2Feature} based on {@link AuditV2Config}.
     *
     * @return a new instance of {@link AuditV2Feature}
     */
    @Override
    public AuditV2Feature get() {
        return AuditV2Feature.create(config);
    }
}
