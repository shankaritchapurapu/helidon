/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.Optional;

import io.helidon.common.context.Contexts;
import io.helidon.common.context.spi.DataPropagationProvider;

/**
 * Propagator for context about a metered activity started and ended in different places in the code.
 */
public class MeteringRegionContextPropagationProvider implements DataPropagationProvider<Optional<MeteringSupport.MeteringRegionContext>> {

    /**
     * Creates a new instance (service loading).
     */
    public MeteringRegionContextPropagationProvider() {
    }

    @Override
    public Optional<MeteringSupport.MeteringRegionContext> data() {
        return Contexts.context().flatMap(c -> c.get(MeteringSupport.MeteringRegionContext.class));
    }

    @Override
    public void propagateData(Optional<MeteringSupport.MeteringRegionContext> meteringRegionContext) {
        meteringRegionContext.ifPresent(c -> Contexts.context().ifPresent(ctx -> ctx.register(c)));
    }

    @Override
    public void clearData(Optional<MeteringSupport.MeteringRegionContext> meteringRegionContext) {
        meteringRegionContext.ifPresent(c -> Contexts.context().ifPresent(ctx -> ctx.unregister(c)));
    }
}
