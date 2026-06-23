/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.oracle.pic.kiev.streams.service.client.config.StreamingConfig;
import com.oracle.pic.kiev.streams.service.client.core.Stream;
import com.oracle.pic.kiev.streams.service.client.core.StreamResult;

/**
 * Registry of configured Kiev data stores.
 */
@Service.Singleton
final class KievDataStores {
    private final Map<String, KievStoreConfig> storeConfigs;
    private final Map<String, DataStoreConfig> dataStoreConfigs;
    private final Map<String, StreamingConfig> streamingConfigs;
    private final KievTransactions transactions;
    private final ConcurrentMap<String, DataStore> dataStores = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MappedDataStore> mappedDataStores = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, KievTransactionSupport> transactionSupports = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Stream> streams = new ConcurrentHashMap<>();

    @Service.Inject
    KievDataStores(KievConfig config,
                   Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
                   KievTransactions transactions,
                   ServiceRegistry serviceRegistry) {
        Map<String, KievStoreConfig> stores = storeConfigs(config);
        this.storeConfigs = Collections.unmodifiableMap(stores);
        this.dataStoreConfigs = Collections.unmodifiableMap(dataStoreConfigs(stores, authProvider, serviceRegistry));
        this.streamingConfigs = Collections.unmodifiableMap(streamingConfigs(stores, authProvider, serviceRegistry));
        this.transactions = transactions;
    }

    KievDataStores(Map<String, KievStoreConfig> storeConfigs,
                   Map<String, DataStoreConfig> dataStoreConfigs,
                   Map<String, StreamingConfig> streamingConfigs,
                   KievTransactions transactions) {
        this.storeConfigs = Collections.unmodifiableMap(storeConfigs);
        this.dataStoreConfigs = Collections.unmodifiableMap(dataStoreConfigs);
        this.streamingConfigs = Collections.unmodifiableMap(streamingConfigs);
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

    /**
     * Store names of the configured streaming clients.
     *
     * @return streaming client store names
     */
    Set<String> streamingStoreNames() {
        return streamingConfigs.keySet();
    }

    IllegalStateException unqualifiedInjectionException(String injectionType) {
        return unqualifiedInjectionException(injectionType, storeNames());
    }

    IllegalStateException unqualifiedStreamingInjectionException() {
        return unqualifiedInjectionException("Stream", streamingStoreNames());
    }

    private IllegalStateException unqualifiedInjectionException(String injectionType,
                                                               Set<String> registeredStoreNames) {
        return new IllegalStateException("Kiev " + injectionType
                                                 + " injection requires @Service.Named to select "
                                                 + "a configured data store. "
                                                 + "Registered store names: "
                                                 + registeredStoreNames(registeredStoreNames)
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
     * Streaming client by store name.
     *
     * @param storeName store name
     * @return streaming client
     */
    Stream stream(String storeName) {
        Objects.requireNonNull(storeName);
        return streams.computeIfAbsent(storeName, key -> new ManagedStream(key, streamingConfig(key).connect()));
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

    StreamingConfig streamingConfig(String storeName) {
        Objects.requireNonNull(storeName);
        StreamingConfig streamingConfig = streamingConfigs.get(storeName);
        if (streamingConfig == null) {
            throw new NoSuchElementException("No Kiev streaming client configured with store-name '" + storeName + "'");
        }
        return streamingConfig;
    }

    @Service.PreDestroy
    void close() {
        IllegalStateException failure = null;
        try {
            closeStreams();
        } catch (IllegalStateException e) {
            failure = e;
        }
        try {
            closeDataStores();
        } catch (IllegalStateException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    void closeDataStores() {
        closeDataStores(dataStores.values());
    }

    void closeStreams() {
        closeStreams(streams.values());
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

    static void closeStreams(Iterable<? extends Stream> streams) {
        IllegalStateException failure = null;
        for (Stream stream : streams) {
            try {
                stream.close();
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to close Kiev streaming clients", e);
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

    private static Map<String, StreamingConfig> streamingConfigs(
            Map<String, KievStoreConfig> storeConfigs,
            Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
            ServiceRegistry serviceRegistry) {
        Map<String, StreamingConfig> configs = new LinkedHashMap<>();
        storeConfigs.forEach((storeName, storeConfig) ->
                KievStreamingClientConfigFactory.create(storeConfig, authProvider, serviceRegistry)
                        .ifPresent(streamingConfig -> configs.put(storeName, streamingConfig)));
        return configs;
    }

    private static String registeredStoreNames(Set<String> storeNames) {
        if (storeNames.isEmpty()) {
            return "<none>";
        }
        return String.join(", ", storeNames);
    }

    private final class ManagedStream implements Stream {
        private final String storeName;
        private final Stream delegate;

        private ManagedStream(String storeName, Stream delegate) {
            this.storeName = storeName;
            this.delegate = delegate;
        }

        @Override
        public String getOldestCursor() {
            return delegate.getOldestCursor();
        }

        @Override
        public String getNewestCursor() {
            return delegate.getNewestCursor();
        }

        @Override
        public String getCursor(CursorType cursorType, Long commitId) {
            return delegate.getCursor(cursorType, commitId);
        }

        @Override
        public StreamResult getRecords(String cursor, Integer limit) {
            return delegate.getRecords(cursor, limit);
        }

        @Override
        public StreamResult getRecords(String cursor, Integer limit, List<String> bucketNames) {
            return delegate.getRecords(cursor, limit, bucketNames);
        }

        @Override
        public StreamResult getLinkedRecords(String cursor, Integer limit) {
            return delegate.getLinkedRecords(cursor, limit);
        }

        @Override
        public void close() {
            try {
                delegate.close();
            } finally {
                streams.remove(storeName, this);
            }
        }
    }
}
