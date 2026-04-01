/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.mapping.MappedDataStore;

/**
 * Factory that exposes a singleton {@link MappedDataStore}.
 */
@Service.Singleton
class KievMappedDataStoreFactory implements Supplier<MappedDataStore> {

    private final DataStore dataStore;
    private volatile MappedDataStore mappedDataStore;

    @Service.Inject
    KievMappedDataStoreFactory(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public synchronized MappedDataStore get() {
        if (mappedDataStore == null) {
            mappedDataStore = new MappedDataStore(dataStore);
        }
        return mappedDataStore;
    }
}
