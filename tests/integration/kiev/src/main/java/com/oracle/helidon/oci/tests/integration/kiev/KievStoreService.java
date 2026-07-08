/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tests.integration.kiev;

import java.util.List;
import java.util.Optional;

import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;

import com.oracle.helidon.oci.kiev.Kiev;
import com.oracle.pic.kiev.Bucket;
import com.oracle.pic.kiev.Transaction;
import com.oracle.pic.kiev.exceptions.DuplicateKeyException;
import com.oracle.pic.kiev.mapping.MappedDataStore;
import com.oracle.pic.kiev.mapping.MappedHashBucket;

/**
 * Kiev store service.
 */
@Service.Singleton
class KievStoreService {
    static final String KIEV_DATA_STORE = "kiev-regional-overlay";
    static final String BUCKET_NAME = "store_example_items";

    private final MappedHashBucket<String, StoreItem> bucket;

    @Service.Inject
    KievStoreService(@Service.Named(KIEV_DATA_STORE) MappedDataStore mappedDataStore) {
        this.bucket = mappedDataStore.getOrCreateBucket(BUCKET_NAME,
                                                        "Helidon OCI Kiev store example bucket",
                                                        String.class,
                                                        StoreItem.class);
    }

    String post(String id, String value) {
        return post(null, id, value);
    }

    @Kiev.Transaction(value = KIEV_DATA_STORE, name = "store-post")
    String post(Transaction tx, String id, String value) {
        try {
            StoreItem item = bucket.insert(tx, new StoreItem(id, value));
            return item.value;
        } catch (DuplicateKeyException e) {
            throw new HttpException("Unable to store item", Status.CONFLICT_409, e);
        }
    }

    String put(String id, String value) {
        return put(null, id, value);
    }

    @Kiev.Transaction(KIEV_DATA_STORE)
    String put(Transaction tx, String id, String value) {
        Optional<StoreItem> existing = bucket.get(tx, id);
        if (existing.isEmpty()) {
            throw new HttpException("No value found for key: " + id, Status.NOT_FOUND_404);
        }
        StoreItem item = bucket.put(tx, new StoreItem(id, value));
        return item.value;
    }

    Optional<String> get(String id) {
        return get(null, id);
    }

    @Kiev.Transaction(KIEV_DATA_STORE)
    Optional<String> get(Transaction tx, String id) {
        return bucket.get(tx, id).map(item -> item.value);
    }

    Optional<String> delete(String id) {
        return delete(null, id);
    }

    @Kiev.Transaction(KIEV_DATA_STORE)
    Optional<String> delete(Transaction tx, String id) {
        Optional<StoreItem> existing = bucket.get(tx, id);
        existing.ifPresent(_ -> bucket.delete(tx, id));
        return existing.map(item -> item.value);
    }

    List<StoreItem> list() {
        return list(null);
    }

    @Kiev.Transaction(value = KIEV_DATA_STORE, readOnly = true)
    List<StoreItem> list(Transaction tx) {
        return bucket.rangeGet(tx, 100, Bucket.Direction.ASCENDING).results();
    }
}
