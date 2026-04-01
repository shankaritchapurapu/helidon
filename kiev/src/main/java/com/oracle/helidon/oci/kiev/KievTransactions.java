/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.NoSuchElementException;
import java.util.Optional;

import io.helidon.common.context.Context;

import com.oracle.pic.kiev.Transaction;

/**
 * Utilities for accessing Kiev transactions associated with a request context.
 */
public final class KievTransactions {
    static final Object CONTEXT_KEY = KievTransactions.class;

    private KievTransactions() {
    }

    /**
     * Current Kiev transaction from the provided context.
     *
     * @param context request context
     * @return current transaction if present
     */
    public static Optional<Transaction> current(Context context) {
        return context.get(CONTEXT_KEY, Transaction.class);
    }

    /**
     * Current Kiev transaction from the provided context.
     *
     * @param context request context
     * @return current transaction
     */
    public static Transaction require(Context context) {
        return current(context)
                .orElseThrow(() -> new NoSuchElementException("No active Kiev transaction in request context"));
    }
}
