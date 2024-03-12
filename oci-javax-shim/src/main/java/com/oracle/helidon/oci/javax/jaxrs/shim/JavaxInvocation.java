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

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class JavaxInvocation implements Invocation {

    private final jakarta.ws.rs.client.Invocation delegate;

    public JavaxInvocation(jakarta.ws.rs.client.Invocation delegate) {
        this.delegate = delegate;
    }

    @Override
    public Invocation property(String name, Object value) {
        return new JavaxInvocation(delegate.property(name, value));
    }

    @Override
    public Response invoke() {
        return new JavaxResponse(delegate.invoke());
    }

    @Override
    public <T> T invoke(Class<T> responseType) {
        return delegate.invoke(responseType);
    }

    @Override
    public <T> T invoke(GenericType<T> responseType) {
        return delegate.invoke(new JakartaGenericType<T>(responseType));
    }

    @Override
    public Future<Response> submit() {
        return FutureUtils.toJavaxResponse(delegate.submit());
    }

    @Override
    public <T> Future<T> submit(Class<T> responseType) {
        return delegate.submit(responseType);
    }

    @Override
    public <T> Future<T> submit(GenericType<T> responseType) {
        return delegate.submit(new JakartaGenericType<T>(responseType));
    }

    @Override
    public <T> Future<T> submit(InvocationCallback<T> callback) {
        return delegate.submit(new JakartaInvocationCallback<T, T>(callback));
    }

    public static class Builder implements Invocation.Builder {

        private final jakarta.ws.rs.client.Invocation.Builder delegate;

        public Builder(jakarta.ws.rs.client.Invocation.Builder delegate) {
            this.delegate = delegate;
        }

        @Override
        public Invocation build(String method) {
            return new JavaxInvocation(delegate.build(method));
        }

        @Override
        public Invocation build(String method, Entity<?> entity) {
            return new JavaxInvocation(delegate.build(method, JaxRsShim.toJakarta(entity)));
        }

        @Override
        public Invocation buildGet() {
            return new JavaxInvocation(delegate.buildGet());
        }

        @Override
        public Invocation buildDelete() {
            return new JavaxInvocation(delegate.buildDelete());
        }

        @Override
        public Invocation buildPost(Entity<?> entity) {
            return new JavaxInvocation(delegate.buildPost(JaxRsShim.toJakarta(entity)));
        }

        @Override
        public Invocation buildPut(Entity<?> entity) {
            return new JavaxInvocation(delegate.buildPut(JaxRsShim.toJakarta(entity)));
        }

        @Override
        public AsyncInvoker async() {
            return new JavaxAsyncInvoker(delegate.async());
        }

        @Override
        public Invocation.Builder accept(String... mediaTypes) {
            return new Builder(delegate.accept(mediaTypes));
        }

        @Override
        public Invocation.Builder accept(MediaType... mediaTypes) {
            return new Builder(delegate.accept(Arrays.stream(mediaTypes)
                                                                       .map(JakartaMediaType::new)
                                                                       .toArray(JakartaMediaType[]::new)));
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
            return new Builder(delegate.cookie(new JakartaCookie(cookie)));
        }

        @Override
        public Invocation.Builder cookie(String name, String value) {
            return new Builder(delegate.cookie(name, value));
        }

        @Override
        public Invocation.Builder cacheControl(CacheControl cacheControl) {
            return new Builder(delegate.cacheControl(new JakartaCacheControl(cacheControl)));
        }

        @Override
        public Invocation.Builder header(String name, Object value) {
            return new Builder(delegate.header(name, value));
        }

        @Override
        public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
            return new Builder(delegate.headers(new JakartaMultivaluedMap<>(headers)));
        }

        @Override
        public Invocation.Builder property(String name, Object value) {
            return new Builder(delegate.property(name, value));
        }

        @Override
        public Response get() {
            return new JavaxResponse(delegate.get());
        }

        @Override
        public <T> T get(Class<T> responseType) {
            return delegate.get(responseType);
        }

        @Override
        public <T> T get(GenericType<T> responseType) {
            return delegate.get(new JakartaGenericType<>(responseType));
        }

        @Override
        public Response put(Entity<?> entity) {
            return new JavaxResponse(delegate.put(JaxRsShim.toJakarta(entity)));
        }

        @Override
        public <T> T put(Entity<?> entity, Class<T> responseType) {
            return delegate.put(JaxRsShim.toJakarta(entity), responseType);
        }

        @Override
        public <T> T put(Entity<?> entity, GenericType<T> responseType) {
            return delegate.put(JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
        }

        @Override
        public Response post(Entity<?> entity) {
            return new JavaxResponse(delegate.post(JaxRsShim.toJakarta(entity)));
        }

        @Override
        public <T> T post(Entity<?> entity, Class<T> responseType) {
            return delegate.post(JaxRsShim.toJakarta(entity), responseType);
        }

        @Override
        public <T> T post(Entity<?> entity, GenericType<T> responseType) {
            return delegate.post(JaxRsShim.toJakarta(entity), new JakartaGenericType<T>(responseType));
        }

        @Override
        public Response delete() {
            return new JavaxResponse(delegate.delete());
        }

        @Override
        public <T> T delete(Class<T> responseType) {
            return delegate.delete(responseType);
        }

        @Override
        public <T> T delete(GenericType<T> responseType) {
            return delegate.delete(new JakartaGenericType<T>(responseType));
        }

        @Override
        public Response head() {
            return new JavaxResponse(delegate.head());
        }

        @Override
        public Response options() {
            return new JavaxResponse(delegate.options());
        }

        @Override
        public <T> T options(Class<T> responseType) {
            return delegate.options(responseType);
        }

        @Override
        public <T> T options(GenericType<T> responseType) {
            return delegate.options(new JakartaGenericType<T>(responseType));
        }

        @Override
        public Response trace() {
            return new JavaxResponse(delegate.trace());
        }

        @Override
        public <T> T trace(Class<T> responseType) {
            return delegate.trace(responseType);
        }

        @Override
        public <T> T trace(GenericType<T> responseType) {
            return delegate.trace(new JakartaGenericType<T>(responseType));
        }

        @Override
        public Response method(String name) {
            return new JavaxResponse(delegate.method(name));
        }

        @Override
        public <T> T method(String name, Class<T> responseType) {
            return delegate.method(name, responseType);
        }

        @Override
        public <T> T method(String name, GenericType<T> responseType) {
            return delegate.method(name, new JakartaGenericType<T>(responseType));
        }

        @Override
        public Response method(String name, Entity<?> entity) {
            return new JavaxResponse(delegate.method(name, JaxRsShim.toJakarta(entity)));
        }

        @Override
        public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
            return delegate.method(name, JaxRsShim.toJakarta(entity), responseType);
        }

        @Override
        public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
            return delegate.method(name, JaxRsShim.toJakarta(entity), new JakartaGenericType<T>(responseType));
        }
    }
}
