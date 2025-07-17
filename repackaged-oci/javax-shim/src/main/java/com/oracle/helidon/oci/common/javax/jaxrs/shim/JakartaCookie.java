/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import jakarta.ws.rs.core.Cookie;

public class JakartaCookie extends Cookie {
    public JakartaCookie(javax.ws.rs.core.Cookie delegate) {
        super(delegate.getName(), delegate.getValue(), delegate.getPath(), delegate.getDomain(), delegate.getVersion());
    }
}
