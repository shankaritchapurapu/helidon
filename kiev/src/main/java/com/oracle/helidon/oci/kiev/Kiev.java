/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.service.registry.Interception;

/**
 * Helidon OCI Kiev annotations.
 */
public final class Kiev {
    private Kiev() {
    }

    /**
     * Marks a method to execute within a Kiev transaction.
     * <p>
     * If the method declares a {@code com.oracle.pic.kiev.Transaction} parameter, generated interception code
     * supplies the active transaction instance to that parameter.
     * If that parameter already contains a transaction managed by Helidon for the configured data store, the method
     * participates in the existing transaction. In that case, the interceptor does not open, commit, abort, or close
     * the transaction. A transaction associated with another data store, or one not managed by Helidon Kiev, is
     * rejected.
     * <p>
     * When there is no existing transaction to reuse, the interceptor opens one before invoking the method. After the
     * method returns normally, a writable transaction is committed; a read-only transaction does not require a commit.
     * The newly opened transaction is always closed after method and transaction processing completes. If processing
     * fails, a writable transaction that is still in flight is aborted before it is closed; a read-only transaction is
     * closed without being aborted. The failure is then propagated to the caller.
     */
    @Interception.Intercepted
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    public @interface Transaction {
        /**
         * Kiev data store name used by this transaction.
         *
         * @return Kiev data store name
         */
        String value();

        /**
         * Transaction base name used for diagnostics. Helidon appends a runtime suffix before opening the Kiev transaction,
         * so explicit names must be at most 58 characters.
         *
         * @return transaction base name, or empty string to use a generated unique default with room for the runtime suffix
         */
        String name() default "";

        /**
         * Whether the transaction should be read only.
         *
         * @return {@code true} for read only transactions
         */
        boolean readOnly() default false;
    }
}
