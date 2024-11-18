/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

public class JakartaContainerRequestFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JakartaContainerRequestFilter.class.getName());

    private final javax.ws.rs.container.ContainerRequestFilter delegate;

    public JakartaContainerRequestFilter(javax.ws.rs.container.ContainerRequestFilter delegate) {
        this.delegate = delegate;
    }

    public JakartaContainerRequestFilter(Class<? extends javax.ws.rs.container.ContainerRequestFilter> delegateClass) {
        try {
            this.delegate = delegateClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        translateExceptions(() -> delegate.filter(new JavaxContainerRequestContext(requestContext)));
    }

    public static void translateExceptions(IoRunnable runnable) throws IOException {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            throw JaxRsShim.toJakarta(e);
        } catch (IOException e){
            throw JaxRsShim.toJakarta(e);
        }
    }

    @FunctionalInterface
    public interface IoRunnable {
        void run() throws IOException;
    }
}
