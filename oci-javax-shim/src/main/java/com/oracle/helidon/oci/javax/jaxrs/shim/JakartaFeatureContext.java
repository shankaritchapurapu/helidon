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

import java.util.Map;

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.FeatureContext;

public class JakartaFeatureContext implements FeatureContext {

    private final javax.ws.rs.core.FeatureContext delegate;

    public JakartaFeatureContext(javax.ws.rs.core.FeatureContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public Configuration getConfiguration() {
        return new JakartaConfiguration(delegate.getConfiguration());
    }

    @Override
    public FeatureContext property(String name, Object value) {
        return new JakartaFeatureContext(delegate.property(name, value));
    }

    @Override
    public FeatureContext register(Class<?> componentClass) {
        return new JakartaFeatureContext(delegate.register(componentClass));
    }

    @Override
    public FeatureContext register(Class<?> componentClass, int priority) {
        return new JakartaFeatureContext(delegate.register(componentClass, priority));
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Class<?>... contracts) {
        return new JakartaFeatureContext(delegate.register(componentClass, contracts));
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return new JakartaFeatureContext(delegate.register(componentClass, contracts));
    }

    @Override
    public FeatureContext register(Object component) {
        return new JakartaFeatureContext(delegate.register(component));
    }

    @Override
    public FeatureContext register(Object component, int priority) {
        return new JakartaFeatureContext(delegate.register(component, priority));
    }

    @Override
    public FeatureContext register(Object component, Class<?>... contracts) {
        return new JakartaFeatureContext(delegate.register(component, contracts));
    }

    @Override
    public FeatureContext register(Object component, Map<Class<?>, Integer> contracts) {
        return new JakartaFeatureContext(delegate.register(component, contracts));
    }
}
