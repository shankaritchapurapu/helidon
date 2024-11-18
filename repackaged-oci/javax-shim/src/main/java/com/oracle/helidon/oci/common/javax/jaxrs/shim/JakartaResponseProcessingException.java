/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

public class JakartaResponseProcessingException extends jakarta.ws.rs.client.ResponseProcessingException {
    public JakartaResponseProcessingException(javax.ws.rs.client.ResponseProcessingException delegate) {
        super(new JakartaResponse(delegate.getResponse()), delegate.getMessage(), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
