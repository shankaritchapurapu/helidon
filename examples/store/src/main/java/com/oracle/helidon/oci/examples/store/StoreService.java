/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;

import com.oracle.helidon.oci.kiev.KievTransaction;
import com.oracle.pic.kiev.Bucket;
import com.oracle.pic.kiev.Transaction;
import com.oracle.pic.kiev.exceptions.DuplicateKeyException;
import com.oracle.pic.kiev.mapping.MappedDataStore;
import com.oracle.pic.kiev.mapping.MappedHashBucket;
import com.oracle.pic.kiev.mapping.Page;

/**
 * Small application service that hides Kiev setup and transaction management from the endpoint.
 */
@Service.Singleton
class StoreService {
    static final String DATA_STORE_NAME = "helidon-store-example";
    static final String BUCKET_NAME = "store_example_items";
    private static final String HASH_KEY_COLUMN = "id";

    private final MappedHashBucket<String, StoreItem> bucket;

    @Service.Inject
    StoreService(@Service.Named(DATA_STORE_NAME) MappedDataStore mappedDataStore) {
        this.bucket = mappedDataStore.getOrCreateBucket(BUCKET_NAME,
                                                        "Helidon OCI Kiev store example bucket",
                                                        String.class,
                                                        StoreItem.class);
    }

    String post(String id, String value) {
        return post(null, id, value);
    }

    @KievTransaction(value = DATA_STORE_NAME, name = "store-post")
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

    @KievTransaction(value = DATA_STORE_NAME, name = "store-put")
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

    @KievTransaction(value = DATA_STORE_NAME, name = "store-get")
    Optional<String> get(Transaction tx, String id) {
        return bucket.get(tx, id).map(item -> item.value);
    }

    Optional<String> delete(String id) {
        return delete(null, id);
    }

    @KievTransaction(value = DATA_STORE_NAME, name = "store-delete")
    Optional<String> delete(Transaction tx, String id) {
        Optional<StoreItem> existing = bucket.get(tx, id);
        existing.ifPresent(item -> bucket.delete(tx, id));
        return existing.map(item -> item.value);
    }

    StorePage list(String pageToken, int pageSize) {
        return list(null, pageToken, pageSize);
    }

    @KievTransaction(value = DATA_STORE_NAME, name = "store-list", readOnly = true)
    StorePage list(Transaction tx, String pageToken, int pageSize) {
        Page<StoreItem> page = pageToken == null
                ? bucket.rangeGet(tx, pageSize, Bucket.Direction.ASCENDING)
                : bucket.rangeGet(tx, decodePageToken(pageToken), pageSize, Bucket.Direction.ASCENDING, Bucket.Bounding.EXCLUSIVE);

        Optional<String> nextPageToken = page.hasNext()
                ? Optional.of(encodePageToken(page.next().keys().getString(HASH_KEY_COLUMN)))
                : Optional.empty();
        return new StorePage(page.results(), nextPageToken);
    }

    private static String encodePageToken(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePageToken(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new HttpException("Invalid page token", Status.BAD_REQUEST_400, e);
        }
    }
}
