/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

import jakarta.ws.rs.BadRequestException;

public class JakartaBadRequestException extends BadRequestException {

    public JakartaBadRequestException(javax.ws.rs.BadRequestException delegate) {
        super(delegate.getMessage(), new JakartaResponse(delegate.getResponse()), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
