/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import jakarta.ws.rs.core.Variant;

public class JakartaVariant extends Variant {
    public JakartaVariant(javax.ws.rs.core.Variant delegate) {
        super(new JakartaMediaType(delegate.getMediaType()), delegate.getLanguage(), delegate.getEncoding());
    }
}
