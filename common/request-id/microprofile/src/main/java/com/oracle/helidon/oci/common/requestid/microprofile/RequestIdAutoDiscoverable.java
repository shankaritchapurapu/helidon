/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.requestid.microprofile;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;

/**
 * Registers REST filters to support request ID headers. For servers or clients running
 * in a server environment that need to propagate request ID headers. This class will
 * be automatically loaded by Jersey using the Java service loader mechanism.
 *
 * @deprecated this is only intended for {@link java.util.ServiceLoader}, this class will be moved to a different module
 */
@Deprecated
public class RequestIdAutoDiscoverable implements AutoDiscoverable {
    /**
     * Required by {@link java.util.ServiceLoader}.
     *
     * @deprecated this is only intended for {@link java.util.ServiceLoader}, this class will be moved to a different module
     */
    @Deprecated
    public RequestIdAutoDiscoverable() {
        super();
    }

    @Override
    public void configure(FeatureContext context) {
        RuntimeType rt = context.getConfiguration().getRuntimeType();
        if (RuntimeType.CLIENT == rt) {
            context.register(RequestIdClientFilter.class);
        }
    }
}
