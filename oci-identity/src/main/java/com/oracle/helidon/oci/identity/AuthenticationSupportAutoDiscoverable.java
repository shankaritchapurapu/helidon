package com.oracle.helidon.oci.identity;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;

public class AuthenticationSupportAutoDiscoverable implements AutoDiscoverable {
    @Override
    public void configure(FeatureContext context) {
        RuntimeType rt = context.getConfiguration().getRuntimeType();
        if (RuntimeType.SERVER == rt) {
            context.register(AuthenticationSupportingFilter.class);
        }
    }
}

