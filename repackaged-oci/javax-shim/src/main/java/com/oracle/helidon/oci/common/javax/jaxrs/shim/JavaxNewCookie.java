/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import javax.ws.rs.core.NewCookie;

public class JavaxNewCookie extends NewCookie {
    public JavaxNewCookie(jakarta.ws.rs.core.NewCookie delegate) {
        super(delegate.getName(),
              delegate.getValue(),
              delegate.getPath(),
              delegate.getDomain(),
              delegate.getVersion(),
              delegate.getComment(),
              delegate.getMaxAge(),
              delegate.getExpiry(),
              delegate.isSecure(),
              delegate.isHttpOnly());
    }
}
