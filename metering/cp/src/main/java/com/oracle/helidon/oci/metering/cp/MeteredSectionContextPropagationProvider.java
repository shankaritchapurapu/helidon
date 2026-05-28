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
public class MeteredSectionContextPropagationProvider implements DataPropagationProvider<Optional<MeteringSupport.MeteredSectionsSnapshot>> {

    /**
     * Creates a new instance (service loading).
     */
    public MeteredSectionContextPropagationProvider() {
    }

    @Override
    public Optional<MeteringSupport.MeteredSectionsSnapshot> data() {
        return Contexts.context()
                .flatMap(c -> c.get(MeteringSupport.MeteredSections.class))
                .map(MeteringSupport.MeteredSections::snapshot);
    }

    @Override
    public void propagateData(Optional<MeteringSupport.MeteredSectionsSnapshot> meteredSectionsSnapshot) {
        meteredSectionsSnapshot.ifPresent(snapshot -> Contexts.context()
                .ifPresent(ctx -> MeteringSupport.replaceMeteredSections(ctx, snapshot)));
    }

    @Override
    public void clearData(Optional<MeteringSupport.MeteredSectionsSnapshot> meteredSectionsSnapshot) {
        meteredSectionsSnapshot.flatMap(ignored -> Contexts.context()).ifPresent(MeteringSupport::clearMeteredSections);
    }
}
