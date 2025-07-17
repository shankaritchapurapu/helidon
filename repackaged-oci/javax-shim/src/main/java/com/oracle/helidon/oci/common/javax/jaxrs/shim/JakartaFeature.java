/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

public class JakartaFeature implements Feature {

    private final javax.ws.rs.core.Feature delegate;

    public JakartaFeature(javax.ws.rs.core.Feature delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean configure(FeatureContext context) {
        return delegate.configure(new JavaxFeatureContext(context));
    }
}
