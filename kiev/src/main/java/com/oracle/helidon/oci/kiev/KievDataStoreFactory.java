/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.DataStoreConfig;

/**
 * Factory that creates and owns the Kiev {@link DataStore}.
 */
@Service.Singleton
class KievDataStoreFactory implements Supplier<DataStore> {

    private final DataStoreConfig dataStoreConfig;
    private volatile DataStore dataStore;

    @Service.Inject
    KievDataStoreFactory(DataStoreConfig dataStoreConfig) {
        this.dataStoreConfig = dataStoreConfig;
    }

    @Override
    public synchronized DataStore get() {
        if (dataStore == null) {
            dataStore = dataStoreConfig.connect();
        }
        return dataStore;
    }

    @Service.PreDestroy
    void closeDataStore() {
        if (dataStore != null) {
            dataStore.close();
        }
    }
}
