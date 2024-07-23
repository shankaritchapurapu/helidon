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

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.concurrent.Future;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

class JavaxAsyncInvoker implements AsyncInvoker {
    private final jakarta.ws.rs.client.AsyncInvoker delegate;

    public JavaxAsyncInvoker(jakarta.ws.rs.client.AsyncInvoker delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Response> get() {
        return FutureUtils.toJavaxResponse(delegate.get());
    }

    @Override
    public <T> Future<T> get(Class<T> responseType) {
        return delegate.get(responseType);
    }

    @Override
    public <T> Future<T> get(GenericType<T> responseType) {
        return delegate.get(new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> get(InvocationCallback<T> callback) {
        return delegate.get(new JakartaInvocationCallback<>(callback));
    }

    @Override
    public Future<Response> put(Entity<?> entity) {
        return FutureUtils.toJavaxResponse(delegate.put(JaxRsShim.toJakarta(entity)));
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, Class<T> responseType) {
        return delegate.put(JaxRsShim.toJakarta(entity), responseType);
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, GenericType<T> responseType) {
        return delegate.put(JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, InvocationCallback<T> callback) {
        return delegate.put(JaxRsShim.toJakarta(entity), new JakartaInvocationCallback<>(callback));
    }

    @Override
    public Future<Response> post(Entity<?> entity) {
        return FutureUtils.toJavaxResponse(delegate.post(JaxRsShim.toJakarta(entity)));
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, Class<T> responseType) {
        return delegate.post(JaxRsShim.toJakarta(entity), responseType);
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, GenericType<T> responseType) {
        return delegate.post(JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, InvocationCallback<T> callback) {
        return delegate.post(JaxRsShim.toJakarta(entity), new JakartaInvocationCallback<>(callback));
    }

    @Override
    public Future<Response> delete() {
        return FutureUtils.toJavaxResponse(delegate.delete());
    }

    @Override
    public <T> Future<T> delete(Class<T> responseType) {
        return delegate.delete(responseType);
    }

    @Override
    public <T> Future<T> delete(GenericType<T> responseType) {
        return delegate.delete(new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> delete(InvocationCallback<T> callback) {
        return delegate.delete(new JakartaInvocationCallback<>(callback));
    }

    @Override
    public Future<Response> head() {
        return FutureUtils.toJavaxResponse(delegate.head());
    }

    @Override
    public Future<Response> head(InvocationCallback<Response> callback) {
        return FutureUtils.toJavaxResponse(delegate.head(new JakartaInvocationCallback<>(callback, JavaxResponse::new)));
    }

    @Override
    public Future<Response> options() {
        return FutureUtils.toJavaxResponse(delegate.options());
    }

    @Override
    public <T> Future<T> options(Class<T> responseType) {
        return delegate.options(responseType);
    }

    @Override
    public <T> Future<T> options(GenericType<T> responseType) {
        return delegate.options(new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> options(InvocationCallback<T> callback) {
        return delegate.options(new JakartaInvocationCallback<>(callback));
    }

    @Override
    public Future<Response> trace() {
        return FutureUtils.toJavaxResponse(delegate.trace());
    }

    @Override
    public <T> Future<T> trace(Class<T> responseType) {
        return delegate.trace(responseType);
    }

    @Override
    public <T> Future<T> trace(GenericType<T> responseType) {
        return delegate.trace(new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> trace(InvocationCallback<T> callback) {
        return delegate.trace(new JakartaInvocationCallback<>(callback));
    }

    @Override
    public Future<Response> method(String name) {
        return FutureUtils.toJavaxResponse(delegate.method(name));
    }

    @Override
    public <T> Future<T> method(String name, Class<T> responseType) {
        return delegate.method(name, responseType);
    }

    @Override
    public <T> Future<T> method(String name, GenericType<T> responseType) {
        return delegate.method(name, new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> method(String name, InvocationCallback<T> callback) {
        return delegate.method(name, new JakartaInvocationCallback<>(callback));
    }

    @Override
    public Future<Response> method(String name, Entity<?> entity) {
        return FutureUtils.toJavaxResponse(delegate.method(name, JaxRsShim.toJakarta(entity)));
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, Class<T> responseType) {
        return delegate.method(name, JaxRsShim.toJakarta(entity), responseType);
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        return delegate.method(name, JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, InvocationCallback<T> callback) {
        return delegate.method(name, JaxRsShim.toJakarta(entity), new JakartaInvocationCallback<>(callback));
    }
}
