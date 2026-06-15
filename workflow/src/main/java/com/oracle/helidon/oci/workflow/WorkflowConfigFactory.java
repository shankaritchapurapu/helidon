/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * A factory to create an instance of {@link WorkflowConfig}.
 *
 * @param config Helidon configuration root
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
record WorkflowConfigFactory(Config config) implements Supplier<WorkflowConfig> {

    /**
     * Get a new instance of {@link WorkflowConfig}.
     *
     * @return a new instance of {@link WorkflowConfig}
     */
    @Override
    public WorkflowConfig get() {
        return WorkflowConfig.create(config.get("oci.workflow"));
    }
}
