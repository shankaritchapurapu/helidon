/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim.shim;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;

public class TestAutoDiscoverable implements AutoDiscoverable {
    @Override
    public void configure(jakarta.ws.rs.core.FeatureContext ctx) {
        JavaxServerFilterTest.configure(ctx);
    }
}
