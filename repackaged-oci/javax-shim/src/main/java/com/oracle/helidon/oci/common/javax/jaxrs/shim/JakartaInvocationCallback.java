/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.function.Function;

import jakarta.ws.rs.client.InvocationCallback;

public class JakartaInvocationCallback<I, O> implements InvocationCallback<O> {

    private final javax.ws.rs.client.InvocationCallback<I> delegate;
    private final Function<O, I> mapper;

    public JakartaInvocationCallback(javax.ws.rs.client.InvocationCallback<I> delegate, Function<O, I> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    public JakartaInvocationCallback(javax.ws.rs.client.InvocationCallback<I> delegate) {
        this.delegate = delegate;
        this.mapper = out -> (I) out;
    }

    @Override
    public void completed(O payload) {
        delegate.completed(mapper.apply(payload));
    }

    @Override
    public void failed(Throwable throwable) {
        delegate.failed(throwable);
    }
}
