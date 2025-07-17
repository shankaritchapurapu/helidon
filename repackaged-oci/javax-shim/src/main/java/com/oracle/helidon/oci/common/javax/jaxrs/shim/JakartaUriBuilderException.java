/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;

public class JakartaUriBuilderException extends jakarta.ws.rs.core.UriBuilderException {
    public JakartaUriBuilderException(javax.ws.rs.core.UriBuilderException delegate) {
        super(delegate.getMessage(), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
    }
}
