/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Kiev stream page.
 *
 * @param records records in the page
 * @param nextCursor cursor to request the next page
 * @param secondsFromTip stream lag in seconds
 */
record StoreStreamPage(List<StreamRecord> records, String nextCursor, long secondsFromTip) {

    /**
     * Kiev stream record summary.
     *
     * @param recordType record type
     * @param beginTxId begin transaction id
     * @param commitTxId commit transaction id
     * @param commitTime commit time
     * @param contents mutation contents
     */
    record StreamRecord(String recordType,
                        Long beginTxId,
                        Long commitTxId,
                        Instant commitTime,
                        List<StreamContent> contents) {
    }

    /**
     * Kiev stream mutation content.
     *
     * @param bucketName bucket name
     * @param mutationType mutation type
     * @param keys mutation keys
     * @param values mutation values
     */
    record StreamContent(String bucketName,
                         String mutationType,
                         Map<String, Object> keys,
                         Map<String, Object> values) {
    }
}
