/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jersey.shim;


import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.jersey.internal.inject.InjectionManager;

/**
 * Provides limited support for javax injection points in JAX-RS resources.
 */
public class JavaxInjectInjectionResolver implements InjectionResolver<javax.inject.Inject> {

    @jakarta.ws.rs.core.Context
    private InjectionManager injectionManager;

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> handle) {
        return injectionManager.getInstance(injectee.getRequiredType());
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
