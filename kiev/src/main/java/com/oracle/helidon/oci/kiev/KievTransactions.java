/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import io.helidon.common.context.Context;
import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.Transaction;

/**
 * Utilities for accessing Kiev transactions associated with a request context.
 */
@Service.Singleton
final class KievTransactions {
    private final Map<Transaction, String> transactionStoreNames = new IdentityHashMap<>();

    @Service.Inject
    KievTransactions() {
    }

    /**
     * Current Kiev transaction from the provided context.
     *
     * @param context request context
     * @param storeName configured data store name
     * @return current transaction if present
     */
    Optional<Transaction> current(Context context, String storeName) {
        Objects.requireNonNull(context);
        return context.get(contextKey(storeName), Transaction.class);
    }

    /**
     * Current Kiev transaction from the provided context.
     *
     * @param context request context
     * @param storeName configured data store name
     * @return current transaction
     */
    Transaction require(Context context, String storeName) {
        return current(context, storeName)
                .orElseThrow(() -> new NoSuchElementException("No active Kiev transaction for store-name '"
                                                                      + storeName + "' in request context"));
    }

    Optional<Transaction> register(Context context, String storeName, Transaction transaction) {
        Object key = contextKey(storeName);
        Optional<Transaction> previous = context.get(key, Transaction.class);
        context.register(key, transaction);
        return previous;
    }

    void unregister(Context context,
                    String storeName,
                    Transaction transaction,
                    Optional<Transaction> previousTransaction) {
        Object key = contextKey(storeName);
        context.unregister(key, transaction);
        previousTransaction.ifPresent(previous -> context.register(key, previous));
    }

    void register(String storeName, Transaction transaction) {
        Objects.requireNonNull(transaction);
        String validatedStoreName = validateStoreName(storeName);
        synchronized (transactionStoreNames) {
            String previous = transactionStoreNames.get(transaction);
            if (previous != null && !previous.equals(validatedStoreName)) {
                throw new IllegalStateException("Kiev transaction is already associated with store-name '"
                                                        + previous + "'");
            }
            transactionStoreNames.put(transaction, validatedStoreName);
        }
    }

    void unregister(String storeName, Transaction transaction) {
        Objects.requireNonNull(transaction);
        synchronized (transactionStoreNames) {
            if (validateStoreName(storeName).equals(transactionStoreNames.get(transaction))) {
                transactionStoreNames.remove(transaction);
            }
        }
    }

    Optional<String> storeName(Transaction transaction) {
        Objects.requireNonNull(transaction);
        synchronized (transactionStoreNames) {
            return Optional.ofNullable(transactionStoreNames.get(transaction));
        }
    }

    boolean isForStore(Transaction transaction, String storeName) {
        return storeName(transaction)
                .filter(validateStoreName(storeName)::equals)
                .isPresent();
    }

    private Object contextKey(String storeName) {
        return new ContextKey(validateStoreName(storeName));
    }

    private static String validateStoreName(String storeName) {
        Objects.requireNonNull(storeName);
        if (storeName.isBlank()) {
            throw new IllegalArgumentException("storeName must not be blank");
        }
        return storeName;
    }

    private record ContextKey(String storeName) {
    }
}
