/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.GenericType;

public class JavaxRxInvoker<T> implements RxInvoker<T> {

    private final jakarta.ws.rs.client.RxInvoker<T> delegate;

    public JavaxRxInvoker(jakarta.ws.rs.client.RxInvoker<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public <R> T get(Class<R> responseType) {
        return delegate.get(responseType);
    }

    @Override
    public <R> T get(GenericType<R> responseType) {
        return delegate.get(new JakartaGenericType<>(responseType));
    }

    @Override
    public T put(Entity<?> entity) {
        return delegate.put(JaxRsShim.toJakarta(entity));
    }

    @Override
    public <R> T put(Entity<?> entity, Class<R> responseType) {
        return delegate.put(JaxRsShim.toJakarta(entity), responseType);
    }

    @Override
    public <R> T put(Entity<?> entity, GenericType<R> responseType) {
        return delegate.put(JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
    }

    @Override
    public T post(Entity<?> entity) {
        return delegate.post(JaxRsShim.toJakarta(entity));
    }

    @Override
    public <R> T post(Entity<?> entity, Class<R> responseType) {
        return delegate.post(JaxRsShim.toJakarta(entity), responseType);
    }

    @Override
    public <R> T post(Entity<?> entity, GenericType<R> responseType) {
        return delegate.post(JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
    }

    @Override
    public T delete() {
        return delegate.delete();
    }

    @Override
    public <R> T delete(Class<R> responseType) {
        return delegate.delete(responseType);
    }

    @Override
    public <R> T delete(GenericType<R> responseType) {
        return delegate.delete(new JakartaGenericType<>(responseType));
    }

    @Override
    public T head() {
        return delegate.head();
    }

    @Override
    public T options() {
        return delegate.options();
    }

    @Override
    public <R> T options(Class<R> responseType) {
        return delegate.options(responseType);
    }

    @Override
    public <R> T options(GenericType<R> responseType) {
        return delegate.options(new JakartaGenericType<>(responseType));
    }

    @Override
    public T trace() {
        return delegate.trace();
    }

    @Override
    public <R> T trace(Class<R> responseType) {
        return delegate.trace(responseType);
    }

    @Override
    public <R> T trace(GenericType<R> responseType) {
        return delegate.trace(new JakartaGenericType<>(responseType));
    }

    @Override
    public T method(String name) {
        return delegate.method(name);
    }

    @Override
    public <R> T method(String name, Class<R> responseType) {
        return delegate.method(name, responseType);
    }

    @Override
    public <R> T method(String name, GenericType<R> responseType) {
        return delegate.method(name, new JakartaGenericType<>(responseType));
    }

    @Override
    public T method(String name, Entity<?> entity) {
        return delegate.method(name, JaxRsShim.toJakarta(entity));
    }

    @Override
    public <R> T method(String name, Entity<?> entity, Class<R> responseType) {
        return delegate.method(name, JaxRsShim.toJakarta(entity), responseType);
    }

    @Override
    public <R> T method(String name, Entity<?> entity, GenericType<R> responseType) {
        return delegate.method(name, JaxRsShim.toJakarta(entity), new JakartaGenericType<>(responseType));
    }
}
