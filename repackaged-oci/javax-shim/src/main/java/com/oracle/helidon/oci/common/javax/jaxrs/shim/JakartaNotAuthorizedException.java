/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.NotAuthorizedException;

public class JakartaNotAuthorizedException extends NotAuthorizedException {
    private final javax.ws.rs.NotAuthorizedException delegate;

    public JakartaNotAuthorizedException(javax.ws.rs.NotAuthorizedException delegate) {
        super(delegate.getMessage(), new JakartaResponse(delegate.getResponse()), JaxRsShim.toJakarta(delegate.getCause()));
        super.setStackTrace(delegate.getStackTrace());
        Arrays.stream(delegate.getSuppressed()).forEach(super::addSuppressed);
        this.delegate = delegate;
    }

    @Override
    public List<Object> getChallenges() {
        return delegate.getChallenges();
    }
}
