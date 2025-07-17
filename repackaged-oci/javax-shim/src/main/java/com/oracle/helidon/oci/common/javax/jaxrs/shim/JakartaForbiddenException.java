/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

public class JakartaForbiddenException extends jakarta.ws.rs.ForbiddenException {
    public JakartaForbiddenException(javax.ws.rs.ForbiddenException delegate) {
        super(delegate.getMessage(), new JakartaResponse(delegate.getResponse()), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
