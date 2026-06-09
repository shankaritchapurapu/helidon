/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Objects;
import java.util.Optional;

import io.helidon.common.context.Context;
import io.helidon.service.registry.Interception;
import io.helidon.service.registry.InterceptionContext;

import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.Transaction;

/**
 * Utility service used by generated and service-level interceptors to manage Kiev transactions.
 */
public class KievTransactionSupport {
    private static final int KIEV_TRANSACTION_NAME_LIMIT = 80;
    private static final int RUNTIME_TRANSACTION_SUFFIX_MAX_LENGTH = 1 + Long.toString(Long.MIN_VALUE).length();
    static final int TRANSACTION_BASE_NAME_MAX_LENGTH = KIEV_TRANSACTION_NAME_LIMIT
            - RUNTIME_TRANSACTION_SUFFIX_MAX_LENGTH
            - 1;

    private final String storeName;
    private final DataStore dataStore;
    private final KievTransactions transactions;

    KievTransactionSupport(String storeName, DataStore dataStore, KievTransactions transactions) {
        this.storeName = storeName;
        this.dataStore = dataStore;
        this.transactions = transactions;
    }

    /**
     * Execute callback within a Kiev transaction.
     *
     * @param transactionName transaction base name; a unique runtime suffix is appended before opening
     *        the transaction, so the base name must be at most 58 characters
     * @param readOnly whether to create a read only transaction
     * @param callback callback to execute
     * @param <T> callback result type
     * @return callback result
     * @throws Exception when callback or transaction lifecycle fails
     */
    public <T> T execute(String transactionName, boolean readOnly, KievTransactionCallback<T> callback) throws Exception {
        Objects.requireNonNull(transactionName, "Kiev transaction name must not be null");
        if (transactionName.length() > TRANSACTION_BASE_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("""
                Kiev transaction name must be at most %d characters because Helidon appends a runtime suffix \
                and Kiev requires the final transaction name to be below %d characters; got %d characters\
                """.formatted(TRANSACTION_BASE_NAME_MAX_LENGTH, KIEV_TRANSACTION_NAME_LIMIT, transactionName.length()));
        }
        String effectiveTransactionName = transactionName + "-" + System.nanoTime();
        Transaction transaction = readOnly
                ? dataStore.beginReadOnlyTransaction(effectiveTransactionName, DataStore.TIMESTAMP_NOW)
                : dataStore.beginTransaction(effectiveTransactionName);
        boolean registered = false;
        try {
            transactions.register(storeName, transaction);
            registered = true;
            T result = callback.execute(transaction);
            if (!readOnly) {
                transaction.commit();
            }
            return result;
        } catch (Exception e) {
            if (!readOnly && transaction.getState() == Transaction.State.IN_FLIGHT) {
                transaction.abort();
            }
            throw e;
        } finally {
            try {
                transaction.close();
            } finally {
                if (registered) {
                    transactions.unregister(storeName, transaction);
                }
            }
        }
    }

    private boolean owns(Transaction transaction) {
        return transactions.isForStore(transaction, storeName);
    }

    /**
     * Base interceptor implementation for methods annotated with {@link KievTransaction}.
     */
    public abstract static class TransactionMethod implements Interception.ElementInterceptor {
        /**
         * Constructor with no side effects.
         */
        protected TransactionMethod() {
        }

        @Override
        public <V> V proceed(InterceptionContext ctx, Chain<V> chain, Object... args) throws Exception {
            KievTransactionSupport transactionSupport = support();
            int parameterIndex = transactionParameterIndex();
            if (parameterIndex >= args.length) {
                throw new IllegalStateException("Transaction parameter index " + parameterIndex
                                                        + " is outside intercepted argument array for "
                                                        + ctx.serviceInfo().serviceType().fqName() + "."
                                                        + ctx.elementInfo().signature().text());
            }

            if (parameterIndex >= 0) {
                Object existing = args[parameterIndex];
                if (existing != null) {
                    if (existing instanceof Transaction transaction && transactionSupport.owns(transaction)) {
                        return chain.proceed(args);
                    }
                    if (existing instanceof Transaction transaction) {
                        throw mismatchedTransactionException(parameterIndex,
                                                             transactionSupport,
                                                             transaction);
                    }
                    throw new IllegalStateException("Expected Kiev transaction argument at index "
                                                            + parameterIndex + " but got "
                                                            + existing.getClass().getName());
                }
            }

            Object[] effectiveArgs = args.clone();
            Context requestContext = requestContext(args);
            return transactionSupport.execute(transactionName(), readOnly(), transaction -> {
                if (parameterIndex >= 0) {
                    effectiveArgs[parameterIndex] = transaction;
                }
                if (requestContext == null) {
                    return chain.proceed(effectiveArgs);
                }

                Optional<Transaction> previousTransaction =
                        transactionSupport.transactions.register(requestContext,
                                                                 transactionSupport.storeName,
                                                                 transaction);
                try {
                    return chain.proceed(effectiveArgs);
                } finally {
                    transactionSupport.transactions.unregister(requestContext,
                                                               transactionSupport.storeName,
                                                               transaction,
                                                               previousTransaction);
                }
            });
        }

        private Context requestContext(Object[] args) {
            for (Object arg : args) {
                if (arg instanceof Context context) {
                    return context;
                }
            }
            return null;
        }

        private IllegalStateException mismatchedTransactionException(int parameterIndex,
                                                                     KievTransactionSupport transactionSupport,
                                                                     Transaction transaction) {
            String prefix = "Kiev transaction argument at index " + parameterIndex;
            String storeName = transactionSupport.storeName;
            return transactionSupport.transactions.storeName(transaction)
                    .<IllegalStateException>map(actualStoreName -> new IllegalStateException(prefix
                            + " is associated with store-name '" + actualStoreName
                            + "', but @KievTransaction requires store-name '" + storeName + "'"))
                    .orElseGet(() -> new IllegalStateException(prefix
                            + " is not managed by Helidon Kiev for store-name '" + storeName + "'"));
        }

        /**
         * Transaction support service.
         *
         * @return support service
         */
        protected abstract KievTransactionSupport support();

        /**
         * Configured transaction name.
         *
         * @return transaction name
         */
        protected abstract String transactionName();

        /**
         * Whether this transaction is read only.
         *
         * @return {@code true} for read only transactions
         */
        protected abstract boolean readOnly();

        /**
         * Index of the {@link Transaction} parameter, or {@code -1} if the method does not declare one.
         *
         * @return transaction parameter index
         */
        protected abstract int transactionParameterIndex();
    }

    /**
     * Callback invoked within a Kiev transaction.
     *
     * @param <T> callback result
     */
    @FunctionalInterface
    public interface KievTransactionCallback<T> {
        /**
         * Execute callback body.
         *
         * @param transaction current transaction
         *
         * @return callback result
         * @throws Exception callback failure
         */
        T execute(Transaction transaction) throws Exception;
    }
}
