/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

import javax.ws.rs.WebApplicationException;

public class JakartaWebApplicationException extends jakarta.ws.rs.WebApplicationException {

    public JakartaWebApplicationException(WebApplicationException delegate) {
        super(delegate.getMessage(), JaxRsShim.toJakarta(delegate.getCause()), new JakartaResponse(delegate.getResponse()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
