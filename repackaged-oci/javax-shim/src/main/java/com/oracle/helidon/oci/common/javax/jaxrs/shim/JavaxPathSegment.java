/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.core.MultivaluedMap;

import jakarta.ws.rs.core.PathSegment;

public class JavaxPathSegment implements javax.ws.rs.core.PathSegment {
    private final PathSegment delegate;

    public JavaxPathSegment(PathSegment delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public MultivaluedMap<String, String> getMatrixParameters() {
        return new JavaxMultivaluedMap<>(delegate.getMatrixParameters());
    }
}
