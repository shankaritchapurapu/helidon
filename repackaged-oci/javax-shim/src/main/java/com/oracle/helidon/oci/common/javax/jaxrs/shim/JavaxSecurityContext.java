/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

public class JavaxSecurityContext implements SecurityContext {
    private final jakarta.ws.rs.core.SecurityContext delegate;

    public JavaxSecurityContext(jakarta.ws.rs.core.SecurityContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public Principal getUserPrincipal() {
        return delegate.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        return delegate.isUserInRole(role);
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return delegate.getAuthenticationScheme();
    }
}
