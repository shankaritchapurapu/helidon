/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import io.helidon.common.context.Context;
import io.helidon.service.registry.Interception;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.Transaction;

/**
 * Utility service used by generated and service-level interceptors to manage Kiev transactions.
 */
@Service.Singleton
public class KievTransactionSupport {
    private final DataStore dataStore;

    @Service.Inject
    KievTransactionSupport(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Execute callback within a Kiev transaction.
     *
     * @param transactionName transaction name
     * @param readOnly whether to create a read only transaction
     * @param callback callback to execute
     * @param <T> callback result type
     * @return callback result
     * @throws Exception when callback or transaction lifecycle fails
     */
    public <T> T execute(String transactionName, boolean readOnly, KievTransactionCallback<T> callback) throws Exception {
        String effectiveTransactionName = transactionName + "-" + System.nanoTime();
        Transaction transaction = readOnly
                ? dataStore.beginReadOnlyTransaction(effectiveTransactionName, DataStore.TIMESTAMP_NOW)
                : dataStore.beginTransaction(effectiveTransactionName);
        try {
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
            transaction.close();
        }
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
                    if (existing instanceof Transaction) {
                        return chain.proceed(args);
                    }
                    throw new IllegalStateException("Expected Kiev transaction argument at index "
                                                            + parameterIndex + " but got "
                                                            + existing.getClass().getName());
                }
            }

            Object[] effectiveArgs = args.clone();
            Context requestContext = requestContext(args);
            return support().execute(transactionName(), readOnly(), transaction -> {
                if (parameterIndex >= 0) {
                    effectiveArgs[parameterIndex] = transaction;
                }
                if (requestContext == null) {
                    return chain.proceed(effectiveArgs);
                }

                requestContext.register(KievTransactions.CONTEXT_KEY, transaction);
                try {
                    return chain.proceed(effectiveArgs);
                } finally {
                    requestContext.unregister(KievTransactions.CONTEXT_KEY, transaction);
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
