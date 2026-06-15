/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * A factory to create an instance of {@link AuditV2Config}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
record AuditV2ConfigFactory(Config config) implements Supplier<AuditV2Config> {

    /**
     * Get a new instance of {@link AuditV2Config}.
     *
     * @return a new instance of {@link AuditV2Config}
     */
    @Override
    public AuditV2Config get() {
        return AuditV2Config.create(config.get("oci.auditv2"));
    }
}
