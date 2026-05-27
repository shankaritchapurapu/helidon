/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import io.helidon.builder.api.Prototype;

final class PkiConfigSupport implements Prototype.BuilderDecorator<PkiConfig.BuilderBase<?, ?>> {
    PkiConfigSupport() {
    }

    @Override
    public void decorate(PkiConfig.BuilderBase<?, ?> builder) {
        if (builder.secretPath().isPresent() && builder.resource().isPresent()) {
            throw new IllegalArgumentException("Only one SSv2 secret or PKI JSON resource can be configured at a time.");
        }
        if (builder.secretPath().isEmpty() && builder.resource().isEmpty()) {
            throw new IllegalArgumentException("Either SSv2 secret or PKI JSON resource must be configured.");
        }
    }
}
