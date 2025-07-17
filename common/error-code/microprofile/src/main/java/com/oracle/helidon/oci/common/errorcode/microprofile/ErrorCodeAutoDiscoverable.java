/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode.microprofile;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;

/**
 * Registers exception mapper to support error code mapping. This class will be
 * automatically loaded by Jersey using the Java service loader mechanism.
 */
@ConstrainedTo(RuntimeType.SERVER)
public class ErrorCodeAutoDiscoverable implements AutoDiscoverable {

    @Override
    public void configure(FeatureContext context) {
        context.register(ErrorCodeExceptionMapper.class);
    }
}
