/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistry;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.DataStoreConfig;
import com.oracle.pic.kiev.mapping.MappedDataStore;

/**
 * Registry of configured Kiev data stores.
 */
@Service.Singleton
final class KievDataStores {
    private final Map<String, KievStoreConfig> storeConfigs;
    private final Map<String, DataStoreConfig> dataStoreConfigs;
    private final KievTransactions transactions;
    private final ConcurrentMap<String, DataStore> dataStores = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MappedDataStore> mappedDataStores = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, KievTransactionSupport> transactionSupports = new ConcurrentHashMap<>();

    @Service.Inject
    KievDataStores(KievConfig config,
                   Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
                   KievTransactions transactions,
                   ServiceRegistry serviceRegistry) {
        Map<String, KievStoreConfig> stores = storeConfigs(config);
        Map<String, DataStoreConfig> configs = dataStoreConfigs(stores, authProvider, serviceRegistry);

        this.storeConfigs = Collections.unmodifiableMap(stores);
        this.dataStoreConfigs = Collections.unmodifiableMap(configs);
        this.transactions = transactions;
    }

    /**
     * Store names of the configured data stores.
     *
     * @return store names
     */
    Set<String> storeNames() {
        return dataStoreConfigs.keySet();
    }

    IllegalStateException unqualifiedInjectionException(String injectionType) {
        return new IllegalStateException("Kiev " + injectionType
                                                 + " injection requires @Service.Named to select a configured data store. "
                                                 + "Registered store names: "
                                                 + String.join(", ", storeNames())
                                                 + ". Add @Service.Named with one of these names.");
    }

    /**
     * Data store by store name.
     *
     * @param storeName store name
     * @return connected data store
     */
    DataStore dataStore(String storeName) {
        Objects.requireNonNull(storeName);
        return dataStores.computeIfAbsent(storeName, key -> dataStoreConfig(key).connect());
    }

    /**
     * Mapped data store by store name.
     *
     * @param storeName store name
     * @return mapped data store
     */
    MappedDataStore mappedDataStore(String storeName) {
        Objects.requireNonNull(storeName);
        return mappedDataStores.computeIfAbsent(storeName, key -> new MappedDataStore(dataStore(key)));
    }

    /**
     * Transaction support by store name.
     *
     * @param storeName store name
     * @return transaction support for the store
     */
    KievTransactionSupport transactionSupport(String storeName) {
        Objects.requireNonNull(storeName);
        return transactionSupports.computeIfAbsent(storeName,
                                                   key -> new KievTransactionSupport(key,
                                                                                     dataStore(key),
                                                                                     transactions));
    }

    /**
     * Helidon Kiev store configuration by store name.
     *
     * @param storeName store name
     * @return Helidon Kiev store configuration
     */
    KievStoreConfig storeConfig(String storeName) {
        Objects.requireNonNull(storeName);
        KievStoreConfig storeConfig = storeConfigs.get(storeName);
        if (storeConfig == null) {
            throw new NoSuchElementException("No Kiev data store configured with store-name '" + storeName + "'");
        }
        return storeConfig;
    }

    DataStoreConfig dataStoreConfig(String storeName) {
        Objects.requireNonNull(storeName);
        DataStoreConfig dataStoreConfig = dataStoreConfigs.get(storeName);
        if (dataStoreConfig == null) {
            throw new NoSuchElementException("No Kiev data store configured with store-name '" + storeName + "'");
        }
        return dataStoreConfig;
    }

    @Service.PreDestroy
    void closeDataStores() {
        closeDataStores(dataStores.values());
    }

    static void closeDataStores(Iterable<? extends DataStore> dataStores) {
        IllegalStateException failure = null;
        for (DataStore dataStore : dataStores) {
            try {
                dataStore.close();
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to close Kiev data stores", e);
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static Map<String, KievStoreConfig> storeConfigs(KievConfig config) {
        Map<String, KievStoreConfig> configs = new LinkedHashMap<>();
        for (KievStoreConfig storeConfig : config.dataStores()) {
            String storeName = storeConfig.storeName();
            if (storeName.isBlank()) {
                throw new IllegalStateException("oci.kiev.data-stores[].store-name must be configured");
            }
            if (configs.containsKey(storeName)) {
                throw new IllegalStateException("Duplicate Kiev data store configured with store-name '"
                                                        + storeName + "'");
            }
            configs.put(storeName, storeConfig);
        }
        if (configs.isEmpty()) {
            throw new IllegalStateException("oci.kiev.data-stores must contain at least one data store");
        }
        return configs;
    }

    private static Map<String, DataStoreConfig> dataStoreConfigs(
            Map<String, KievStoreConfig> storeConfigs,
            Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
            ServiceRegistry serviceRegistry) {
        Map<String, DataStoreConfig> configs = new LinkedHashMap<>();
        storeConfigs.forEach((storeName, storeConfig) -> {
            DataStoreConfig dataStoreConfig = KievDataStoreConfigFactory.create(storeConfig,
                                                                                authProvider,
                                                                                serviceRegistry);
            configs.put(storeName, dataStoreConfig);
        });
        return configs;
    }

}
