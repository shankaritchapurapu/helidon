/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.service.registry.Interception;

/**
 * Helidon OCI Identity annotations.
 * <p>
 * This class exposes authentication-only interception through {@link Authenticated}.
 * Endpoints that require permission checks should use supported Auth SDK
 * authorization annotations, such as {@code @AuthorizationPermission}; those
 * annotations are also recognized by OCI authorization code generation.
 */
public final class Identity {
    private Identity() {
    }

    /**
     * Marks an endpoint type or method for authentication-only processing.
     * <p>
     * The generated interceptor authenticates the request and registers an
     * {@link IdentityContext} in the request context, enabling identity-related
     * method-parameter injection. This annotation does not perform a service-side
     * authorization call.
     */
    @Interception.Intercepted
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Authenticated {
    }
}
