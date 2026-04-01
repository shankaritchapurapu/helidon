/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Date;
import java.util.List;

import com.oracle.pic.kiev.Bucket;
import com.oracle.pic.kiev.BucketDescription;
import com.oracle.pic.kiev.ChecksumAlgorithm;
import com.oracle.pic.kiev.ColumnDescription;
import com.oracle.pic.kiev.ColumnSetDescription;
import com.oracle.pic.kiev.ComputeBucketChecksumsJobStatus;
import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.DataStoreConfigBase;
import com.oracle.pic.kiev.Transaction;
import com.oracle.pic.kiev.Sequence;
import com.oracle.pic.kiev.SequenceDescription;
import com.oracle.pic.kiev.exceptions.BucketAlreadyExists;
import com.oracle.pic.kiev.exceptions.BucketNotFound;
import com.oracle.pic.kiev.exceptions.ColumnAlreadyExists;
import com.oracle.pic.kiev.exceptions.CommitConflictException;
import com.oracle.pic.kiev.exceptions.DuplicateKeyException;
import com.oracle.pic.kiev.exceptions.IndexAlreadyExists;
import com.oracle.pic.kiev.exceptions.IndexNotFound;
import com.oracle.pic.kiev.exceptions.SequenceAlreadyExists;
import com.oracle.pic.kiev.exceptions.SequenceNotFound;
import com.oracle.pic.kiev.exceptions.UniqueIndexCreationFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KievTransactionSupportTest {

    @Test
    void testCommitsWriteTxn() throws Exception {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = new KievTransactionSupport(dataStore);

        String result = support.execute("store-put", false, current -> {
            assertSame(transaction, current);
            return "done";
        });

        assertEquals("done", result);
        assertEquals("store-put", dataStore.beginTransactionBaseName());
        assertNotEquals("store-put", dataStore.lastWriteTransactionName);
        assertTrue(dataStore.lastWriteTransactionName.startsWith("store-put-"));
        assertEquals(1, transaction.commitCalls);
        assertEquals(0, transaction.abortCalls);
        assertEquals(1, transaction.closeCalls);
        assertFalse(dataStore.readOnlyStarted);
    }

    @Test
    void testAbortsInFlightTxn() {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = new KievTransactionSupport(dataStore);
        IllegalStateException failure = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                                                    () -> support.execute("store-put", false, current -> {
                                                        throw failure;
                                                    }));

        assertSame(failure, thrown);
        assertEquals(0, transaction.commitCalls);
        assertEquals(1, transaction.abortCalls);
        assertEquals(1, transaction.closeCalls);
    }

    @Test
    void testSkipsAbortForFailedTxn() {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.FAILED);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = new KievTransactionSupport(dataStore);

        assertThrows(IllegalStateException.class,
                     () -> support.execute("store-put", false, current -> {
                         throw new IllegalStateException("boom");
                     }));

        assertEquals(0, transaction.commitCalls);
        assertEquals(0, transaction.abortCalls);
        assertEquals(1, transaction.closeCalls);
    }

    @Test
    void testUsesReadOnlyTxn() throws Exception {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = new KievTransactionSupport(dataStore);

        String result = support.execute("store-get", true, current -> {
            assertSame(transaction, current);
            return "value";
        });

        assertEquals("value", result);
        assertEquals("store-get", dataStore.beginReadOnlyBaseName());
        assertNotEquals("store-get", dataStore.lastReadOnlyTransactionName);
        assertTrue(dataStore.lastReadOnlyTransactionName.startsWith("store-get-"));
        assertTrue(dataStore.readOnlyStarted);
        assertEquals(DataStore.TIMESTAMP_NOW, dataStore.lastReadOnlyTimestamp);
        assertEquals(0, transaction.commitCalls);
        assertEquals(0, transaction.abortCalls);
        assertEquals(1, transaction.closeCalls);
    }

    private static final class FakeDataStore implements DataStore {
        private final FakeTransaction transaction;
        private String lastWriteTransactionName;
        private String lastReadOnlyTransactionName;
        private long lastReadOnlyTimestamp;
        private boolean readOnlyStarted;

        private FakeDataStore(FakeTransaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public Transaction beginTransaction(String transactionName) {
            this.lastWriteTransactionName = transactionName;
            return transaction;
        }

        @Override
        public Transaction beginReadOnlyTransaction(String transactionName, long timestamp) {
            this.lastReadOnlyTransactionName = transactionName;
            this.lastReadOnlyTimestamp = timestamp;
            this.readOnlyStarted = true;
            return transaction;
        }

        String beginTransactionBaseName() {
            return lastWriteTransactionName.substring(0, lastWriteTransactionName.lastIndexOf('-'));
        }

        String beginReadOnlyBaseName() {
            return lastReadOnlyTransactionName.substring(0, lastReadOnlyTransactionName.lastIndexOf('-'));
        }

        @Override
        public Bucket createBucket(BucketDescription bucketDescription) throws BucketAlreadyExists, IndexAlreadyExists {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<BucketDescription> enumerateBuckets() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bucket getBucket(String bucketName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addColumnToBucket(String bucketName, ColumnDescription columnDescription) throws ColumnAlreadyExists {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addIndexToBucket(String bucketName, ColumnSetDescription indexDescription)
                throws IndexAlreadyExists, UniqueIndexCreationFailedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean dropBucket(String bucketName) throws BucketNotFound {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean dropIndexForBucket(String bucketName, String indexName) throws BucketNotFound, IndexNotFound {
            throw new UnsupportedOperationException();
        }

        @Override
        public ComputeBucketChecksumsJobStatus computeBucketChecksums(ChecksumAlgorithm checksumAlgorithm,
                                                                      List<com.oracle.pic.kiev.BucketWithPreferredColumns> buckets) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ComputeBucketChecksumsJobStatus> listComputeBucketChecksumsJobStatuses(
                ComputeBucketChecksumsJobStatus.BucketChecksumStatusCode statusCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ComputeBucketChecksumsJobStatus getComputeBucketChecksumsJobStatus(String jobId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ComputeBucketChecksumsJobStatus cancelComputeBucketChecksumsJob(String jobId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataStoreConfigBase getConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }

        @Override
        public long getCurrentTransactionID() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Sequence createSequence(SequenceDescription sequenceDescription) throws SequenceAlreadyExists {
            throw new UnsupportedOperationException();
        }

        @Override
        public Sequence getSequence(String sequenceName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SequenceDescription> enumerateSequences() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean dropSequence(String sequenceName) throws SequenceNotFound {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getUsedConnectionCount() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTransaction implements Transaction {
        private final State state;
        private int commitCalls;
        private int abortCalls;
        private int closeCalls;

        private FakeTransaction(State state) {
            this.state = state;
        }

        @Override
        public void setTrace(boolean trace) {
        }

        @Override
        public void dump() {
        }

        @Override
        public void abort() {
            abortCalls++;
        }

        @Override
        public void commit() throws DuplicateKeyException, CommitConflictException {
            commitCalls++;
        }

        @Override
        public long getTransactionID() {
            return 1L;
        }

        @Override
        public long getStartTransactionID() {
            return 1L;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public Date getBeginTime() {
            return new Date();
        }

        @Override
        public Date getEndTime() {
            return new Date();
        }

        @Override
        public void addCallbackOnCommitSuccess(Runnable callback) {
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
