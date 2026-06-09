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
 * Marks a method to execute within a Kiev transaction.
 * If the method declares a {@code com.oracle.pic.kiev.Transaction} parameter, generated interception code
 * supplies the active transaction instance to that parameter.
 */
@Interception.Intercepted
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface KievTransaction {
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
