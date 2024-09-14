/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import jakarta.ws.rs.core.Response;

public class JakartaStatusType implements Response.StatusType {
    private final javax.ws.rs.core.Response.StatusType delegate;

    public JakartaStatusType(javax.ws.rs.core.Response.StatusType delegate) {
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
