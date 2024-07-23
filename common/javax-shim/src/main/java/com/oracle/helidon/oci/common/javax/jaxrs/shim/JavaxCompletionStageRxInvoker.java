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

import java.util.concurrent.CompletionStage;

import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

public class JavaxCompletionStageRxInvoker implements CompletionStageRxInvoker {
    private final jakarta.ws.rs.client.CompletionStageRxInvoker delegate;

    public JavaxCompletionStageRxInvoker(jakarta.ws.rs.client.CompletionStageRxInvoker delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletionStage<Response> get() {
        return delegate.get().thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> get(Class<T> responseType) {
        return delegate.get(responseType);
    }

    @Override
    public <T> CompletionStage<T> get(GenericType<T> responseType) {
        return delegate.get(new JakartaGenericType<>(responseType));
    }

    @Override
    public CompletionStage<Response> put(Entity<?> entity) {
        return delegate.put(JaxRsShim.toJakarta(entity)).thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> put(Entity<?> entity, Class<T> clazz) {
        return delegate.put(JaxRsShim.toJakarta(entity), clazz);
    }

    @Override
    public <T> CompletionStage<T> put(Entity<?> entity, GenericType<T> type) {
        return delegate.put(JaxRsShim.toJakarta(entity), new JakartaGenericType<>(type));
    }

    @Override
    public CompletionStage<Response> post(Entity<?> entity) {
        return delegate.post(JaxRsShim.toJakarta(entity)).thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> post(Entity<?> entity, Class<T> clazz) {
        return delegate.post(JaxRsShim.toJakarta(entity), clazz);
    }

    @Override
    public <T> CompletionStage<T> post(Entity<?> entity, GenericType<T> type) {
        return delegate.post(JaxRsShim.toJakarta(entity), new JakartaGenericType<>(type));
    }

    @Override
    public CompletionStage<Response> delete() {
        return delegate.delete().thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> delete(Class<T> responseType) {
        return delegate.delete(responseType);
    }

    @Override
    public <T> CompletionStage<T> delete(GenericType<T> responseType) {
        return delegate.delete(new JakartaGenericType<>(responseType));
    }

    @Override
    public CompletionStage<Response> head() {
        return delegate.head().thenApply(JavaxResponse::new);
    }

    @Override
    public CompletionStage<Response> options() {
        return delegate.options().thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> options(Class<T> responseType) {
        return delegate.options(responseType);
    }

    @Override
    public <T> CompletionStage<T> options(GenericType<T> responseType) {
        return delegate.options(new JakartaGenericType<>(responseType));
    }

    @Override
    public CompletionStage<Response> trace() {
        return delegate.trace().thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> trace(Class<T> responseType) {
        return delegate.trace(responseType);
    }

    @Override
    public <T> CompletionStage<T> trace(GenericType<T> responseType) {
        return delegate.trace(new JakartaGenericType<>(responseType));
    }

    @Override
    public CompletionStage<Response> method(String name) {
        return delegate.method(name).thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> method(String name, Class<T> responseType) {
        return delegate.method(name, responseType);
    }

    @Override
    public <T> CompletionStage<T> method(String name, GenericType<T> responseType) {
        return delegate.method(name, new JakartaGenericType<>(responseType));
    }

    @Override
    public CompletionStage<Response> method(String name, Entity<?> entity) {
        return delegate.method(name, JaxRsShim.toJakarta(entity)).thenApply(JavaxResponse::new);
    }

    @Override
    public <T> CompletionStage<T> method(String name, Entity<?> entity, Class<T> responseType) {
        return delegate.method(name, JaxRsShim.toJakarta(entity), responseType);
    }

    @Override
    public <T> CompletionStage<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        return delegate.method(name, JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
    }
}
