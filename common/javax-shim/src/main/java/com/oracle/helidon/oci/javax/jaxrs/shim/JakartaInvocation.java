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

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Future;

import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.CompletionStageRxInvoker;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class JakartaInvocation implements Invocation {

    private final javax.ws.rs.client.Invocation delegate;

    public JakartaInvocation(javax.ws.rs.client.Invocation delegate) {
        this.delegate = delegate;
    }

    @Override
    public Invocation property(String name, Object value) {
        return new JakartaInvocation(delegate.property(name, value));
    }

    @Override
    public Response invoke() {
        return new JakartaResponse(delegate.invoke());
    }

    @Override
    public <T> T invoke(Class<T> responseType) {
        return delegate.invoke(responseType);
    }

    @Override
    public <T> T invoke(GenericType<T> responseType) {
        return delegate.invoke(new JavaxGenericType<T>(responseType));
    }

    @Override
    public Future<Response> submit() {
        return FutureUtils.toJakartaResponse(delegate.submit());
    }

    @Override
    public <T> Future<T> submit(Class<T> responseType) {
        return delegate.submit(responseType);
    }

    @Override
    public <T> Future<T> submit(GenericType<T> responseType) {
        return delegate.submit(new JavaxGenericType<T>(responseType));
    }

    @Override
    public <T> Future<T> submit(InvocationCallback<T> callback) {
        return delegate.submit(new JavaxInvocationCallback<T, T>(callback));
    }

    public static class Builder implements Invocation.Builder {

        private final javax.ws.rs.client.Invocation.Builder delegate;

        public Builder(javax.ws.rs.client.Invocation.Builder delegate) {
            this.delegate = delegate;
        }

        @Override
        public Invocation build(String method) {
            return new JakartaInvocation(delegate.build(method));
        }

        @Override
        public Invocation build(String method, Entity<?> entity) {
            return new JakartaInvocation(delegate.build(method, JaxRsShim.toJavax(entity)));
        }

        @Override
        public Invocation buildGet() {
            return new JakartaInvocation(delegate.buildGet());
        }

        @Override
        public Invocation buildDelete() {
            return new JakartaInvocation(delegate.buildDelete());
        }

        @Override
        public Invocation buildPost(Entity<?> entity) {
            return new JakartaInvocation(delegate.buildPost(JaxRsShim.toJavax(entity)));
        }

        @Override
        public Invocation buildPut(Entity<?> entity) {
            return new JakartaInvocation(delegate.buildPut(JaxRsShim.toJavax(entity)));
        }

        @Override
        public AsyncInvoker async() {
            return new JakartaAsyncInvoker(delegate.async());
        }

        @Override
        public Invocation.Builder accept(String... mediaTypes) {
            return new Builder(delegate.accept(mediaTypes));
        }

        @Override
        public Invocation.Builder accept(MediaType... mediaTypes) {
            return new Builder(delegate.accept(Arrays.stream(mediaTypes)
                                                       .map(JavaxMediaType::new)
                                                       .toArray(JavaxMediaType[]::new)));
        }

        @Override
        public Invocation.Builder acceptLanguage(Locale... locales) {
            return new Builder(delegate.acceptLanguage(locales));
        }

        @Override
        public Invocation.Builder acceptLanguage(String... locales) {
            return new Builder(delegate.acceptLanguage(locales));
        }

        @Override
        public Invocation.Builder acceptEncoding(String... encodings) {
            return new Builder(delegate.acceptEncoding(encodings));
        }

        @Override
        public Invocation.Builder cookie(Cookie cookie) {
            return new Builder(delegate.cookie(new JavaxCookie(cookie)));
        }

        @Override
        public Invocation.Builder cookie(String name, String value) {
            return new Builder(delegate.cookie(name, value));
        }

        @Override
        public Invocation.Builder cacheControl(CacheControl cacheControl) {
            return new Builder(delegate.cacheControl(new JavaxCacheControl(cacheControl)));
        }

        @Override
        public Invocation.Builder header(String name, Object value) {
            return new Builder(delegate.header(name, value));
        }

        @Override
        public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
            return new Builder(delegate.headers(new JavaxMultivaluedMap<>(headers)));
        }

        @Override
        public Invocation.Builder property(String name, Object value) {
            return new Builder(delegate.property(name, value));
        }

        @Override
        public CompletionStageRxInvoker rx() {
            throw new UnsupportedOperationException("Not implemeted!");
        }

        @Override
        public <T extends RxInvoker> T rx(Class<T> clazz) {
            throw new UnsupportedOperationException("Not implemeted!");
        }

        @Override
        public Response get() {
            return new JakartaResponse(delegate.get());
        }

        @Override
        public <T> T get(Class<T> responseType) {
            return delegate.get(responseType);
        }

        @Override
        public <T> T get(GenericType<T> responseType) {
            return delegate.get(new JavaxGenericType<>(responseType));
        }

        @Override
        public Response put(Entity<?> entity) {
            return new JakartaResponse(delegate.put(JaxRsShim.toJavax(entity)));
        }

        @Override
        public <T> T put(Entity<?> entity, Class<T> responseType) {
            return delegate.put(JaxRsShim.toJavax(entity), responseType);
        }

        @Override
        public <T> T put(Entity<?> entity, GenericType<T> responseType) {
            return delegate.put(JaxRsShim.toJavax(entity), new JavaxGenericType<>(responseType));
        }

        @Override
        public Response post(Entity<?> entity) {
            return new JakartaResponse(delegate.post(JaxRsShim.toJavax(entity)));
        }

        @Override
        public <T> T post(Entity<?> entity, Class<T> responseType) {
            return delegate.post(JaxRsShim.toJavax(entity), responseType);
        }

        @Override
        public <T> T post(Entity<?> entity, GenericType<T> responseType) {
            return delegate.post(JaxRsShim.toJavax(entity), new JavaxGenericType<T>(responseType));
        }

        @Override
        public Response delete() {
            return new JakartaResponse(delegate.delete());
        }

        @Override
        public <T> T delete(Class<T> responseType) {
            return delegate.delete(responseType);
        }

        @Override
        public <T> T delete(GenericType<T> responseType) {
            return delegate.delete(new JavaxGenericType<T>(responseType));
        }

        @Override
        public Response head() {
            return new JakartaResponse(delegate.head());
        }

        @Override
        public Response options() {
            return new JakartaResponse(delegate.options());
        }

        @Override
        public <T> T options(Class<T> responseType) {
            return delegate.options(responseType);
        }

        @Override
        public <T> T options(GenericType<T> responseType) {
            return delegate.options(new JavaxGenericType<T>(responseType));
        }

        @Override
        public Response trace() {
            return new JakartaResponse(delegate.trace());
        }

        @Override
        public <T> T trace(Class<T> responseType) {
            return delegate.trace(responseType);
        }

        @Override
        public <T> T trace(GenericType<T> responseType) {
            return delegate.trace(new JavaxGenericType<T>(responseType));
        }

        @Override
        public Response method(String name) {
            return new JakartaResponse(delegate.method(name));
        }

        @Override
        public <T> T method(String name, Class<T> responseType) {
            return delegate.method(name, responseType);
        }

        @Override
        public <T> T method(String name, GenericType<T> responseType) {
            return delegate.method(name, new JavaxGenericType<T>(responseType));
        }

        @Override
        public Response method(String name, Entity<?> entity) {
            return new JakartaResponse(delegate.method(name, JaxRsShim.toJavax(entity)));
        }

        @Override
        public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
            return delegate.method(name, JaxRsShim.toJavax(entity), responseType);
        }

        @Override
        public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
            return delegate.method(name, JaxRsShim.toJavax(entity), new JavaxGenericType<T>(responseType));
        }
    }
}
