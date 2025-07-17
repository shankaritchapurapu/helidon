/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;

public class JavaxRuntimeDelegate extends RuntimeDelegate {

    private final jakarta.ws.rs.ext.RuntimeDelegate delegate;

    public JavaxRuntimeDelegate() {
        this.delegate = jakarta.ws.rs.ext.RuntimeDelegate.getInstance();
    }

    public JavaxRuntimeDelegate(jakarta.ws.rs.ext.RuntimeDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public UriBuilder createUriBuilder() {
        return new JavaxUriBuilder(delegate.createUriBuilder());
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return new JavaxResponse.JavaxResponseBuilder(delegate.createResponseBuilder());
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        return new JavaxVariant.JavaxVariantListBuilder(delegate.createVariantListBuilder());
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        return delegate.createEndpoint(new JakartaApplication(application), endpointType);
    }

    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) throws IllegalArgumentException {
        return new JavaxRuntimeDelegate.JavaxHeaderDelegate<>(delegate.createHeaderDelegate(type));
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return null;
    }

    public static class JavaxHeaderDelegate<T> implements HeaderDelegate<T> {
        private final jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate<T> delegate;

        public JavaxHeaderDelegate(jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T fromString(String value) {
            return delegate.fromString(value);
        }

        @Override
        public String toString(T value) {
            return delegate.toString(value);
        }
    }
}
