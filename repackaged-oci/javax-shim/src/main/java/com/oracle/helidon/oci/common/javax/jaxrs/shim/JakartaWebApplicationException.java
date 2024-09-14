/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.WebApplicationException;

public class JakartaWebApplicationException extends jakarta.ws.rs.WebApplicationException {

    public JakartaWebApplicationException(WebApplicationException delegate) {
        super(delegate.getMessage(), delegate, new JakartaResponse(delegate.getResponse()));
    }
}
