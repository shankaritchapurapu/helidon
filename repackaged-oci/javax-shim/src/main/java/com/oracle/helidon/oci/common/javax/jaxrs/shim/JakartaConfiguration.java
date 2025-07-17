/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;

@SuppressWarnings("CPD-START")
public class JakartaConfiguration implements Configuration {

    private final javax.ws.rs.core.Configuration delegate;

    public JakartaConfiguration(javax.ws.rs.core.Configuration delegate) {
        this.delegate = delegate;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.valueOf(delegate.getRuntimeType().name());
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return delegate.isEnabled(new JavaxFeature(feature));
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> featureClass) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isRegistered(Object component) {
        return delegate.isRegistered(component);
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return delegate.isRegistered(componentClass);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        return delegate.getContracts(componentClass);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return delegate.getClasses();
    }

    @Override
    public Set<Object> getInstances() {
        return delegate.getInstances();
    }
}
