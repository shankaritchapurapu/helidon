/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Map;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.FeatureContext;

public class JavaxFeatureContext implements FeatureContext {

    private final jakarta.ws.rs.core.FeatureContext delegate;

    public JavaxFeatureContext(jakarta.ws.rs.core.FeatureContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public Configuration getConfiguration() {
        return new JavaxConfiguration(delegate.getConfiguration());
    }

    @Override
    public FeatureContext property(String name, Object value) {
        return new JavaxFeatureContext(delegate.property(name, value));
    }

    @Override
    public FeatureContext register(Class<?> componentClass) {
        return new JavaxFeatureContext(delegate.register(componentClass));
    }

    @Override
    public FeatureContext register(Class<?> componentClass, int priority) {
        return new JavaxFeatureContext(delegate.register(componentClass, priority));
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Class<?>... contracts) {
        return new JavaxFeatureContext(delegate.register(componentClass, contracts));
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return new JavaxFeatureContext(delegate.register(componentClass, contracts));
    }

    @Override
    public FeatureContext register(Object component) {
        return new JavaxFeatureContext(delegate.register(component));
    }

    @Override
    public FeatureContext register(Object component, int priority) {
        return new JavaxFeatureContext(delegate.register(component, priority));
    }

    @Override
    public FeatureContext register(Object component, Class<?>... contracts) {
        return new JavaxFeatureContext(delegate.register(component, contracts));
    }

    @Override
    public FeatureContext register(Object component, Map<Class<?>, Integer> contracts) {
        return new JavaxFeatureContext(delegate.register(component, contracts));
    }
}
