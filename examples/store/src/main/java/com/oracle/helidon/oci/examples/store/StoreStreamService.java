/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.ColumnValueSet;
import com.oracle.pic.kiev.streams.service.client.core.Record;
import com.oracle.pic.kiev.streams.service.client.core.SchemaUpdateCommitRecord;
import com.oracle.pic.kiev.streams.service.client.core.Stream;
import com.oracle.pic.kiev.streams.service.client.core.StreamResult;
import com.oracle.pic.kiev.streams.service.client.core.TxCommitContent;
import com.oracle.pic.kiev.streams.service.client.core.TxCommitRecord;

/**
 * Small application service that demonstrates reading Kiev stream records for the configured store.
 */
@Service.Singleton
class StoreStreamService {
    private static final List<String> STREAM_BUCKET_NAMES = List.of(StoreService.BUCKET_NAME);

    private final Optional<Stream> stream;

    @Service.Inject
    StoreStreamService(@Service.Named(StoreService.DATA_STORE_NAME) Optional<Stream> stream) {
        this.stream = stream;
    }

    StoreStreamCursors cursors() {
        Stream stream = stream();
        return new StoreStreamCursors(stream.getOldestCursor(), stream.getNewestCursor());
    }

    StoreStreamPage get(String cursor, int limit) {
        StreamResult result = stream().getRecords(cursor, limit, STREAM_BUCKET_NAMES);
        List<StoreStreamPage.StreamRecord> records = result.getRecords()
                .stream()
                .map(StoreStreamService::toStoreStreamRecord)
                .flatMap(Optional::stream)
                .toList();
        return new StoreStreamPage(records, result.getNextCursor(), result.getSecondsFromTip());
    }

    private Stream stream() {
        return stream
                .orElseThrow(() -> new HttpException("Kiev streams require the store example to use the SERVICE backend",
                                                     Status.NOT_FOUND_404));
    }

    private static Optional<StoreStreamPage.StreamRecord> toStoreStreamRecord(Record record) {
        List<StoreStreamPage.StreamContent> contents = contents(record);
        if (contents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new StoreStreamPage.StreamRecord(record.getRecordType().name(),
                                                           record.getBeginTxId(),
                                                           record.getCommitTxId(),
                                                           record.getCommitTime(),
                                                           contents));
    }

    private static List<StoreStreamPage.StreamContent> contents(Record record) {
        if (record instanceof TxCommitRecord txCommitRecord) {
            return contents(txCommitRecord.getContents());
        }
        if (record instanceof SchemaUpdateCommitRecord schemaUpdateCommitRecord) {
            return contents(schemaUpdateCommitRecord.getContents());
        }
        return List.of();
    }

    private static List<StoreStreamPage.StreamContent> contents(List<TxCommitContent> contents) {
        return contents.stream()
                .filter(content -> StoreService.BUCKET_NAME.equals(content.getBucketName()))
                .map(content -> new StoreStreamPage.StreamContent(content.getBucketName(),
                                                                  content.getTxCommitType().name(),
                                                                  valueMap(content.getKeys()),
                                                                  valueMap(content.getValues())))
                .toList();
    }

    private static Map<String, Object> valueMap(ColumnValueSet values) {
        if (values == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values.getValueMap()));
    }
}
