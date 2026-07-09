/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Atomically admits sets of related keys while enforcing a hard upper bound on distinct admitted keys.
 *
 * @param <T> key type
 */
final class BoundedCardinalityLimiter<T> {
    private final int limit;
    private final Set<T> admitted = new HashSet<>();

    BoundedCardinalityLimiter(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Cardinality limit must be greater than zero");
        }
        this.limit = limit;
    }

    synchronized boolean tryAdmit(Collection<? extends T> keys) {
        Set<T> newKeys = new HashSet<>(keys);
        newKeys.removeAll(admitted);
        if (admitted.size() + newKeys.size() > limit) {
            return false;
        }
        admitted.addAll(newKeys);
        return true;
    }

    synchronized int size() {
        return admitted.size();
    }
}
