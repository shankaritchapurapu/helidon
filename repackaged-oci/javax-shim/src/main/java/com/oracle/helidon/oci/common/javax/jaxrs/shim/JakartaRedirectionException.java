/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

public class JakartaRedirectionException extends jakarta.ws.rs.RedirectionException {
    private final javax.ws.rs.RedirectionException delegate;

    public JakartaRedirectionException(javax.ws.rs.RedirectionException delegate) {
        super(delegate.getMessage(), new JakartaResponse(delegate.getResponse()));
        this.delegate = delegate;
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }

    @Override
    public String getMessage() {
        return delegate.getMessage();
    }
}
