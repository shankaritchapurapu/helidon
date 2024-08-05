/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import jakarta.ws.rs.core.Cookie;

public class JavaxCookie extends javax.ws.rs.core.Cookie {
    public JavaxCookie(Cookie delegate) {
        super(delegate.getName(), delegate.getValue(), delegate.getPath(), delegate.getDomain(), delegate.getVersion());
    }
}
