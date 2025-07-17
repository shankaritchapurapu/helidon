/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.lang.reflect.Method;

import javax.ws.rs.container.ResourceInfo;

public class JavaxResourceInfo implements ResourceInfo {

    private final jakarta.ws.rs.container.ResourceInfo delegate;

    public JavaxResourceInfo(jakarta.ws.rs.container.ResourceInfo delegate) {
        this.delegate = delegate;
    }

    @Override
    public Method getResourceMethod() {
        return delegate.getResourceMethod();
    }

    @Override
    public Class<?> getResourceClass() {
        return delegate.getResourceClass();
    }
}
