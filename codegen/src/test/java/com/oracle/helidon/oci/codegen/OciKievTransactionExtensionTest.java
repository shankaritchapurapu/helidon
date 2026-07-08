/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import io.helidon.codegen.CodegenException;
import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.codegen.OciKievTransactionExtension.TRANSACTION_BASE_NAME_MAX_LENGTH;
import static com.oracle.helidon.oci.codegen.OciKievTransactionExtension.defaultTransactionName;
import static com.oracle.helidon.oci.codegen.OciKievTransactionExtension.explicitTransactionName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OciKievTransactionExtensionTest {
    private static final String LONG_METHOD_NAME = "com.oracle.helidon.oci.tests.integration.kiev.KievStoreService"
            + ".delete(com.oracle.pic.kiev.Transaction,java.lang.String)";

    @Test
    void testDefaultTransactionNameLeavesRoomForRuntimeSuffix() {
        String transactionName = defaultTransactionName(LONG_METHOD_NAME, "veryLongGeneratedMethodName");

        assertTrue(transactionName.length() <= TRANSACTION_BASE_NAME_MAX_LENGTH);
        assertTrue((transactionName + "-" + Long.MIN_VALUE).length() < 80);
    }

    @Test
    void testDefaultTransactionNameIsStable() {
        String transactionName = defaultTransactionName(LONG_METHOD_NAME, "delete");

        assertEquals("kt-delete-762c662d81d41704", transactionName);
    }

    @Test
    void testDefaultTransactionNameIsUniqueForOverloadedMethods() {
        String first = defaultTransactionName("example.Store.get(java.lang.String)", "get");
        String second = defaultTransactionName("example.Store.get(java.lang.Long)", "get");

        assertNotEquals(first, second);
        assertTrue(first.startsWith("kt-get-"));
        assertTrue(second.startsWith("kt-get-"));
    }

    @Test
    void testExplicitTransactionNameLeavesRoomForRuntimeSuffix() {
        String transactionName = "x".repeat(TRANSACTION_BASE_NAME_MAX_LENGTH);

        assertEquals(transactionName, explicitTransactionName(transactionName, "put()"));
        assertTrue((transactionName + "-" + Long.MIN_VALUE).length() < 80);
    }

    @Test
    void testExplicitTransactionNameRejectsTooLongName() {
        String transactionName = "x".repeat(TRANSACTION_BASE_NAME_MAX_LENGTH + 1);
        CodegenException exception = assertThrows(CodegenException.class,
                                                  () -> explicitTransactionName(transactionName, "put()"));

        assertEquals("""
                @Kiev.Transaction name on put() must be at most %d characters because Helidon appends a runtime suffix \
                and Kiev requires the final transaction name to be below 80 characters; got %d characters\
                """.formatted(TRANSACTION_BASE_NAME_MAX_LENGTH,
                               transactionName.length()),
                     exception.getMessage());
    }
}
