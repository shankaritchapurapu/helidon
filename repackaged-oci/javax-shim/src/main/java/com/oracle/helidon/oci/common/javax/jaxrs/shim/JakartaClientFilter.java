/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

public class JakartaClientFilter implements ClientRequestFilter {

    private final javax.ws.rs.client.ClientRequestFilter delegate;

    public JakartaClientFilter(Class<?> delegateclass) {
        try {
            delegate = (javax.ws.rs.client.ClientRequestFilter) delegateclass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public JakartaClientFilter(javax.ws.rs.client.ClientRequestFilter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        delegate.filter(new JavaxClientRequestContext(requestContext));
    }
}
