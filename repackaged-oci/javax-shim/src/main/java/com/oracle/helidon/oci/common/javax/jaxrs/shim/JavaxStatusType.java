/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.core.Response;

public class JavaxStatusType implements Response.StatusType {

    private final jakarta.ws.rs.core.Response.StatusType delegate;

    public JavaxStatusType(jakarta.ws.rs.core.Response.StatusType delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getStatusCode() {
        return delegate.getStatusCode();
    }

    @Override
    public Response.Status.Family getFamily() {
        return Response.Status.Family.valueOf(delegate.getFamily().name());
    }

    @Override
    public String getReasonPhrase() {
        return delegate.getReasonPhrase();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
