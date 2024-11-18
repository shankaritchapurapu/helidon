/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jersey.shim;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;

/**
 * Provides limited support for javax injection points in JAX-RS resources.
 */
@ConstrainedTo(RuntimeType.SERVER)
public class JavaxInjectionAutodiscoverable implements AutoDiscoverable {
    @Override
    public void configure(FeatureContext context) {
        RuntimeType rt = context.getConfiguration().getRuntimeType();
        if (RuntimeType.SERVER == rt) {
            context.register(JavaxShimBinder.class);
        }
    }
}
