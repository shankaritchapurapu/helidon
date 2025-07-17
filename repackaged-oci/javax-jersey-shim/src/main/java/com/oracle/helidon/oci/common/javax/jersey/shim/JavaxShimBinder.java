/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jersey.shim;

import javax.inject.Inject;

import jakarta.inject.Singleton;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Provides limited support for javax injection points in JAX-RS resources.
 */
public class JavaxShimBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(JavaxContextInjectionResolver.class)
                .to(new TypeLiteral<InjectionResolver<javax.ws.rs.core.Context>>(){})
                .in(Singleton.class);

        bind(JavaxInjectInjectionResolver.class)
                .to(new TypeLiteral<InjectionResolver<Inject>>(){})
                .in(Singleton.class);
    }
}
