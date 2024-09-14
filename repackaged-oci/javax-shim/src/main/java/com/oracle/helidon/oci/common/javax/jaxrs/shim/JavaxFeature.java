/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class JavaxFeature implements Feature {

    private final jakarta.ws.rs.core.Feature delegate;

    public JavaxFeature(jakarta.ws.rs.core.Feature delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean configure(FeatureContext context) {
        return delegate.configure(new JakartaFeatureContext(context));
    }
}
