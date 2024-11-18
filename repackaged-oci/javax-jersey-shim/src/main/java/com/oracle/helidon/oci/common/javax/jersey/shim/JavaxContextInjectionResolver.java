/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jersey.shim;


import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxResourceInfo;
import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxUriInfo;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.jersey.internal.inject.InjectionManager;

/**
 * Provides limited support for javax injection points in JAX-RS resources.
 */
public class JavaxContextInjectionResolver implements InjectionResolver<javax.ws.rs.core.Context> {

    @jakarta.ws.rs.core.Context
    private InjectionManager injectionManager;

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> handle) {
        if (javax.ws.rs.container.ResourceInfo.class == injectee.getRequiredType()) {
            return new JavaxResourceInfo(injectionManager.getInstance(jakarta.ws.rs.container.ResourceInfo.class));
        }
        if (javax.ws.rs.core.UriInfo.class == injectee.getRequiredType()) {
            return new JavaxUriInfo(injectionManager.getInstance(jakarta.ws.rs.core.UriInfo.class));
        }
        return null;
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
