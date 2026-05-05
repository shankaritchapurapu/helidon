/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import io.helidon.metrics.api.Tag;

record OciTag(String key, String value) implements Tag {

    @Override
    public <R> R unwrap(Class<? extends R> c) {
        if (c.isInstance(this)) {
            return c.cast(this);
        }
        throw new IllegalArgumentException("Unsupported unwrap type: " + c.getName());
    }
}
