/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.javax.jaxrs.shim;

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
