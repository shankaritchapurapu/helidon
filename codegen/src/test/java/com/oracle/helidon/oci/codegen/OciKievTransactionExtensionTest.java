/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OciKievTransactionExtensionTest {
    private static final String LONG_METHOD_NAME = "com.oracle.helidon.oci.tests.integration.kiev.KievStoreService"
            + ".delete(com.oracle.pic.kiev.Transaction,java.lang.String)";

    @Test
    void testDefaultTransactionNameLeavesRoomForRuntimeSuffix() {
        String transactionName = OciKievTransactionExtension.defaultTransactionName(LONG_METHOD_NAME,
                                                                                   "VeryLongGeneratedKievStoreServiceName",
                                                                                   "veryLongGeneratedMethodName");

        assertTrue(transactionName.length() <= 58);
        assertTrue((transactionName + "-" + Long.MIN_VALUE).length() < 80);
    }

    @Test
    void testDefaultTransactionNameIsStable() {
        String transactionName = OciKievTransactionExtension.defaultTransactionName(LONG_METHOD_NAME,
                                                                                   "KievStoreService",
                                                                                   "delete");

        assertEquals("kt-KievStoreService-delete-762c662d81d41704", transactionName);
    }

    @Test
    void testDefaultTransactionNameIsUniqueForOverloadedMethods() {
        String first = OciKievTransactionExtension.defaultTransactionName("example.Store.get(java.lang.String)",
                                                                          "Store",
                                                                          "get");
        String second = OciKievTransactionExtension.defaultTransactionName("example.Store.get(java.lang.Long)",
                                                                           "Store",
                                                                           "get");

        assertNotEquals(first, second);
        assertTrue(first.startsWith("kt-Store-get-"));
        assertTrue(second.startsWith("kt-Store-get-"));
    }
}
