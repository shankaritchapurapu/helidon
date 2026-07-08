/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Date;
import java.util.List;

import io.helidon.common.context.Context;

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

import static com.oracle.helidon.oci.kiev.KievTransactionSupport.TRANSACTION_BASE_NAME_MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KievTransactionSupportTest {
    private final KievTransactions transactions = new KievTransactions();

    @Test
    void testCommitsWriteTxn() throws Exception {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = transactionSupport("store", dataStore);

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
    void testAcceptsLongestTransactionNameThatLeavesRoomForRuntimeSuffix() throws Exception {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = transactionSupport("store", dataStore);
        String transactionName = "x".repeat(TRANSACTION_BASE_NAME_MAX_LENGTH);

        support.execute(transactionName, false, current -> "done");

        assertEquals(transactionName, dataStore.beginTransactionBaseName());
        assertTrue(dataStore.lastWriteTransactionName.length() < 80);
    }

    @Test
    void testRejectsNullTransactionNameBeforeOpeningTransaction() {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = transactionSupport("store", dataStore);

        NullPointerException exception = assertThrows(NullPointerException.class,
                                                      () -> support.execute(null, false, current -> "done"));

        assertEquals("Kiev transaction name must not be null", exception.getMessage());
        assertEquals(0, dataStore.writeTransactionCalls);
        assertFalse(dataStore.readOnlyStarted);
    }

    @Test
    void testRejectsTooLongTransactionNameBeforeOpeningTransaction() {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = transactionSupport("store", dataStore);
        String transactionName = "x".repeat(TRANSACTION_BASE_NAME_MAX_LENGTH + 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> support.execute(transactionName, false, current -> "done"));

        assertEquals("""
                Kiev transaction name must be at most %d characters because Helidon appends a runtime suffix \
                and Kiev requires the final transaction name to be below 80 characters; got %d characters\
                """.formatted(TRANSACTION_BASE_NAME_MAX_LENGTH,
                               transactionName.length()),
                     exception.getMessage());
        assertEquals(0, dataStore.writeTransactionCalls);
        assertFalse(dataStore.readOnlyStarted);
    }

    @Test
    void testAbortsInFlightTxn() {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = transactionSupport("store", dataStore);
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
        KievTransactionSupport support = transactionSupport("store", dataStore);

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
        KievTransactionSupport support = transactionSupport("store", dataStore);

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

    @Test
    void testReusesExistingTransactionOnlyForSameStore() throws Exception {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(transaction);
        KievTransactionSupport support = transactionSupport("primary-store", dataStore);
        TestTransactionMethod method = new TestTransactionMethod(support, 0);

        String result = support.execute("outer", false, tx -> method.proceed(null, args -> {
            assertSame(tx, args[0]);
            return "done";
        }, tx));

        assertEquals("done", result);
        assertEquals(1, dataStore.writeTransactionCalls);
    }

    @Test
    void testRejectsExistingTransactionFromDifferentStore() throws Exception {
        FakeTransaction primaryTransaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore primaryDataStore = new FakeDataStore(primaryTransaction);
        KievTransactionSupport primarySupport = transactionSupport("primary-store", primaryDataStore);
        FakeTransaction secondaryTransaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore secondaryDataStore = new FakeDataStore(secondaryTransaction);
        KievTransactionSupport secondarySupport = transactionSupport("secondary-store", secondaryDataStore);
        TestTransactionMethod secondaryMethod = new TestTransactionMethod(secondarySupport, 0);

        primarySupport.execute("outer", false, tx -> {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                                                    () -> secondaryMethod.proceed(null, args -> "unexpected", tx));
            assertEquals("Kiev transaction argument at index 0 is associated with store-name 'primary-store', "
                                 + "but @Kiev.Transaction requires store-name 'secondary-store'",
                         ex.getMessage());
            return null;
        });

        assertEquals(1, primaryDataStore.writeTransactionCalls);
        assertEquals(0, secondaryDataStore.writeTransactionCalls);
    }

    @Test
    void testRejectsUnmanagedExistingTransaction() {
        FakeTransaction transaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        FakeDataStore dataStore = new FakeDataStore(new FakeTransaction(Transaction.State.IN_FLIGHT));
        KievTransactionSupport support = transactionSupport("primary-store", dataStore);
        TestTransactionMethod method = new TestTransactionMethod(support, 0);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                                                () -> method.proceed(null, args -> "unexpected", transaction));

        assertEquals("Kiev transaction argument at index 0 is not managed by Helidon Kiev "
                             + "for store-name 'primary-store'",
                     ex.getMessage());
        assertEquals(0, dataStore.writeTransactionCalls);
    }

    @Test
    void testRequestContextTracksTransactionsByStore() throws Exception {
        Context context = Context.create();
        FakeTransaction primaryTransaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        KievTransactionSupport primarySupport =
                transactionSupport("primary-store", new FakeDataStore(primaryTransaction));
        TestTransactionMethod primaryMethod = new TestTransactionMethod(primarySupport, -1);
        FakeTransaction secondaryTransaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        KievTransactionSupport secondarySupport =
                transactionSupport("secondary-store", new FakeDataStore(secondaryTransaction));
        TestTransactionMethod secondaryMethod = new TestTransactionMethod(secondarySupport, -1);

        String result = primaryMethod.proceed(null, primaryArgs -> {
            assertSame(primaryTransaction, transactions.require(context, "primary-store"));
            assertTrue(transactions.current(context, "secondary-store").isEmpty());

            String secondaryResult = secondaryMethod.proceed(null, secondaryArgs -> {
                assertSame(primaryTransaction, transactions.require(context, "primary-store"));
                assertSame(secondaryTransaction, transactions.require(context, "secondary-store"));
                return "done";
            }, context);

            assertSame(primaryTransaction, transactions.require(context, "primary-store"));
            assertTrue(transactions.current(context, "secondary-store").isEmpty());
            return secondaryResult;
        }, context);

        assertEquals("done", result);
        assertTrue(transactions.current(context, "primary-store").isEmpty());
        assertTrue(transactions.current(context, "secondary-store").isEmpty());
    }

    @Test
    void testRequestContextRestoresNestedTransactionForSameStore() throws Exception {
        Context context = Context.create();
        FakeTransaction outerTransaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        KievTransactionSupport outerSupport =
                transactionSupport("primary-store", new FakeDataStore(outerTransaction));
        TestTransactionMethod outerMethod = new TestTransactionMethod(outerSupport, -1);
        FakeTransaction innerTransaction = new FakeTransaction(Transaction.State.IN_FLIGHT);
        KievTransactionSupport innerSupport =
                transactionSupport("primary-store", new FakeDataStore(innerTransaction));
        TestTransactionMethod innerMethod = new TestTransactionMethod(innerSupport, -1);

        String result = outerMethod.proceed(null, outerArgs -> {
            assertSame(outerTransaction, transactions.require(context, "primary-store"));

            String innerResult = innerMethod.proceed(null, innerArgs -> {
                assertSame(innerTransaction, transactions.require(context, "primary-store"));
                return "done";
            }, context);

            assertSame(outerTransaction, transactions.require(context, "primary-store"));
            return innerResult;
        }, context);

        assertEquals("done", result);
        assertTrue(transactions.current(context, "primary-store").isEmpty());
    }

    private KievTransactionSupport transactionSupport(String storeName, FakeDataStore dataStore) {
        return new KievTransactionSupport(storeName, dataStore, transactions);
    }

    private static final class TestTransactionMethod extends KievTransactionSupport.TransactionMethod {
        private final KievTransactionSupport support;
        private final int transactionParameterIndex;

        private TestTransactionMethod(KievTransactionSupport support, int transactionParameterIndex) {
            this.support = support;
            this.transactionParameterIndex = transactionParameterIndex;
        }

        @Override
        protected KievTransactionSupport support() {
            return support;
        }

        @Override
        protected String transactionName() {
            return "test-transaction";
        }

        @Override
        protected boolean readOnly() {
            return false;
        }

        @Override
        protected int transactionParameterIndex() {
            return transactionParameterIndex;
        }
    }

    private static final class FakeDataStore implements DataStore {
        private final FakeTransaction transaction;
        private String lastWriteTransactionName;
        private String lastReadOnlyTransactionName;
        private long lastReadOnlyTimestamp;
        private boolean readOnlyStarted;
        private int writeTransactionCalls;

        private FakeDataStore(FakeTransaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public Transaction beginTransaction(String transactionName) {
            this.lastWriteTransactionName = transactionName;
            this.writeTransactionCalls++;
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
