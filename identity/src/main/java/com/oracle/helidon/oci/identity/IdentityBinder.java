/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import com.oracle.pic.identity.authorization.sdk.context.AuthorizationRequestContextFactoryProvider;
import com.oracle.pic.identity.authorization.sdk.context.NbacRequestContextFactoryProvider;
import com.oracle.pic.identity.authorization.sdk.context.PrincipalContextFactoryProvider;
import com.oracle.pic.identity.authorization.sdk.context.ZprRequestContextFactoryProvider;
import jakarta.inject.Singleton;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

/**
 * Bind JAX-RS parameter providers.
 */
public class IdentityBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(PrincipalContextFactoryProvider.class)
                .to(ValueParamProvider.class)
                .in(Singleton.class);

        bind(AuthorizationRequestContextFactoryProvider.class)
                .to(ValueParamProvider.class)
                .in(Singleton.class);

        bind(NbacRequestContextFactoryProvider.class)
                .to(ValueParamProvider.class)
                .in(Singleton.class);

        bind(ZprRequestContextFactoryProvider.class)
                .to(ValueParamProvider.class)
                .in(Singleton.class);
    }
}
