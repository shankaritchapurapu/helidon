/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

public class JakartaNotAcceptableException extends jakarta.ws.rs.NotAcceptableException {
    public JakartaNotAcceptableException(javax.ws.rs.NotAcceptableException delegate) {
        super(delegate.getMessage(), new JakartaResponse(delegate.getResponse()), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
