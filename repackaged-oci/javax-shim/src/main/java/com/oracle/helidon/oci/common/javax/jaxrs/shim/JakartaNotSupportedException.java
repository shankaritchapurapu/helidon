/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

public class JakartaNotSupportedException extends jakarta.ws.rs.NotSupportedException {
    public JakartaNotSupportedException(javax.ws.rs.NotSupportedException delegate) {
        super(delegate.getMessage(), new JakartaResponse(delegate.getResponse()), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
