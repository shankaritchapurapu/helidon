/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

import jakarta.ws.rs.NotFoundException;

public class JakartaNotFoundException extends NotFoundException {

    public JakartaNotFoundException(javax.ws.rs.NotFoundException delegate) {
        super(delegate.getMessage(), new JakartaResponse(delegate.getResponse()), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
