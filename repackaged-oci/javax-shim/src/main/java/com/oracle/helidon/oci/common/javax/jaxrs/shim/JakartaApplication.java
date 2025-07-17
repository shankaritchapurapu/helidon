/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

public class JakartaApplication extends jakarta.ws.rs.core.Application {
    private final Application delegate;

    public JakartaApplication(Application delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return delegate.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return delegate.getSingletons();
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }
}
