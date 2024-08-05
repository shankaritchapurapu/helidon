/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.core.GenericType;

public class JavaxGenericType<T> extends GenericType<T> {

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public JavaxGenericType(jakarta.ws.rs.core.GenericType<T> delegate) {
    }
}
