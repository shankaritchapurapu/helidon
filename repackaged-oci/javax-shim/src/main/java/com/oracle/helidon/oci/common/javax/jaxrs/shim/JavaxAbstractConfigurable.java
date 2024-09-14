/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Map;

import javax.ws.rs.core.Configurable;

import com.oracle.bmc.http.client.RequestInterceptor;

interface JavaxAbstractConfigurable<C extends Configurable<?>> extends Configurable<C> {

    jakarta.ws.rs.core.Configurable<?> delegate();

    C self();

    @Override
    default C register(Class<?> componentClass) {
        if (javax.ws.rs.client.ClientRequestFilter.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaClientFilter(componentClass));
            return self();
        }

        if (RequestInterceptor.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaRequestInterceptor(componentClass));
            return self();
        }

        delegate().register(componentClass);
        return self();
    }

    @Override
    default C register(Class<?> componentClass, int priority) {
        if (javax.ws.rs.client.ClientRequestFilter.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaClientFilter(componentClass), priority);
            return self();
        }

        if (RequestInterceptor.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaRequestInterceptor(componentClass), priority);
            return self();
        }

        delegate().register(componentClass, priority);
        return self();
    }

    @Override
    default C register(Class<?> componentClass, Class<?>... contracts) {
        if (javax.ws.rs.client.ClientRequestFilter.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaClientFilter(componentClass), contracts);
            return self();
        }

        if (RequestInterceptor.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaRequestInterceptor(componentClass), contracts);
            return self();
        }

        delegate().register(componentClass, contracts);
        return self();
    }

    @Override
    default C register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        if (javax.ws.rs.client.ClientRequestFilter.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaClientFilter(componentClass), contracts);
            return self();
        }

        if (RequestInterceptor.class.isAssignableFrom(componentClass)) {
            delegate().register(new JakartaRequestInterceptor(componentClass), contracts);
            return self();
        }

        delegate().register(componentClass, contracts);
        return self();
    }

    @Override
    default C register(Object component) {
        if (component instanceof javax.ws.rs.client.ClientRequestFilter filter) {
            delegate().register(new JakartaClientFilter(filter));
            return self();
        }

        if (component instanceof RequestInterceptor interceptor) {
            delegate().register(new JakartaRequestInterceptor(interceptor));
            return self();
        }

        delegate().register(component);
        return self();
    }

    @Override
    default C register(Object component, int priority) {
        if (component instanceof javax.ws.rs.client.ClientRequestFilter filter) {
            delegate().register(new JakartaClientFilter(filter), priority);
            return self();
        }

        if (component instanceof RequestInterceptor interceptor) {
            delegate().register(new JakartaRequestInterceptor(interceptor), priority);
            return self();
        }

        delegate().register(component, priority);
        return self();
    }

    @Override
    default C register(Object component, Class<?>... contracts) {
        if (component instanceof javax.ws.rs.client.ClientRequestFilter filter) {
            delegate().register(new JakartaClientFilter(filter), contracts);
            return self();
        }

        if (component instanceof RequestInterceptor interceptor) {
            delegate().register(new JakartaRequestInterceptor(interceptor), contracts);
            return self();
        }

        delegate().register(component, contracts);
        return self();
    }

    @Override
    default C register(Object component, Map<Class<?>, Integer> contracts) {
        if (component instanceof javax.ws.rs.client.ClientRequestFilter filter) {
            delegate().register(new JakartaClientFilter(filter), contracts);
            return self();
        }

        if (component instanceof RequestInterceptor interceptor) {
            delegate().register(new JakartaRequestInterceptor(interceptor), contracts);
            return self();
        }

        delegate().register(component, contracts);
        return self();
    }
}
