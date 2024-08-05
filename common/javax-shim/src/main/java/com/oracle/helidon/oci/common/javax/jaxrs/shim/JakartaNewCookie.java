/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import jakarta.ws.rs.core.NewCookie;

class JakartaNewCookie extends NewCookie {
    public JakartaNewCookie(javax.ws.rs.core.NewCookie delegate) {
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
