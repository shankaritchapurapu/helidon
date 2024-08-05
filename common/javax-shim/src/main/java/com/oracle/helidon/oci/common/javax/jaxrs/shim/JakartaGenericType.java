/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import jakarta.ws.rs.core.GenericType;

public class JakartaGenericType<T> extends GenericType<T> {

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public JakartaGenericType(javax.ws.rs.core.GenericType<T> delegate) {
    }
}
