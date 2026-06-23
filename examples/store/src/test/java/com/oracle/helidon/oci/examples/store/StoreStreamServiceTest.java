/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.oracle.pic.kiev.ColumnValueSet;
import com.oracle.pic.kiev.streams.service.client.core.Record;
import com.oracle.pic.kiev.streams.service.client.core.Stream;
import com.oracle.pic.kiev.streams.service.client.core.StreamResult;
import com.oracle.pic.kiev.streams.service.client.core.TxCommitContent;
import com.oracle.pic.kiev.streams.service.client.core.TxCommitRecord;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoreStreamServiceTest {

    @Test
    void testReadsOnlyExampleBucket() {
        TestStream stream = new TestStream();
        StoreStreamService service = new StoreStreamService(Optional.of(stream));

        StoreStreamPage page = service.get("cursor-1", 10);

        assertThat(stream.cursor, is("cursor-1"));
        assertThat(stream.limit, is(10));
        assertThat(stream.bucketNames, is(List.of(StoreService.BUCKET_NAME)));
        assertThat(page.nextCursor(), is("cursor-2"));
        assertThat(page.secondsFromTip(), is(3L));
        assertThat(page.records().size(), is(1));

        StoreStreamPage.StreamRecord record = page.records().get(0);
        assertThat(record.contents().size(), is(1));

        StoreStreamPage.StreamContent content = record.contents().get(0);
        assertThat(content.bucketName(), is(StoreService.BUCKET_NAME));
        assertThat(content.keys(), is(ColumnValueSet.newWithString("id", "alpha").getValueMap()));
        assertThat(content.values(), is(ColumnValueSet.newWithString("value", "one").getValueMap()));

        stream.values.withString("value", "changed");
        assertThat(content.values().get("value"), is("one"));
        assertThrows(UnsupportedOperationException.class, () -> content.values().put("value", "changed"));
    }

    private static TxCommitContent content(String bucketName, ColumnValueSet keys, ColumnValueSet values) {
        return TxCommitContent.builder()
                .bucketName(bucketName)
                .txCommitType(TxCommitContent.TxCommitType.UPSERT)
                .keys(keys)
                .values(values)
                .build();
    }

    private static final class TestStream implements Stream {
        private String cursor;
        private Integer limit;
        private List<String> bucketNames;
        private final ColumnValueSet keys = ColumnValueSet.newWithString("id", "alpha");
        private final ColumnValueSet values = ColumnValueSet.newWithString("value", "one");

        @Override
        public String getOldestCursor() {
            return "oldest";
        }

        @Override
        public String getNewestCursor() {
            return "newest";
        }

        @Override
        public String getCursor(CursorType cursorType, Long commitId) {
            return cursorType + "-" + commitId;
        }

        @Override
        public StreamResult getRecords(String cursor, Integer limit) {
            throw new AssertionError("StoreStreamService must use the bucket-filtered getRecords overload");
        }

        @Override
        public StreamResult getRecords(String cursor, Integer limit, List<String> bucketNames) {
            this.cursor = cursor;
            this.limit = limit;
            this.bucketNames = bucketNames;

            List<TxCommitContent> contents = List.of(
                    content(StoreService.BUCKET_NAME, keys, values),
                    content("other_bucket",
                            ColumnValueSet.newWithString("id", "secret"),
                            ColumnValueSet.newWithString("value", "other")));
            Record record = new TxCommitRecord(1L, 2L, Instant.EPOCH, contents);
            return StreamResult.of("cursor-2", 3L, List.of(record));
        }

        @Override
        public StreamResult getLinkedRecords(String cursor, Integer limit) {
            throw new UnsupportedOperationException("Not used by this example");
        }

        @Override
        public void close() {
        }
    }
}
