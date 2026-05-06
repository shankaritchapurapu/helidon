/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistryException;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.mapping.MappedDataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KievDataStoreInjectionTest {
    private ServiceRegistryManager registryManager;

    @AfterEach
    void shutdownServices() {
        if (registryManager != null) {
            registryManager.shutdown();
        }
    }

    @Test
    void testInjectsNamedKievServices() {
        configureRegistry(multipleDataStoresConfig());

        NamedKievInjectionService service = Services.get(NamedKievInjectionService.class);

        assertNotNull(service.dataStore());
        assertNotNull(service.mappedDataStore());
        assertNotNull(service.transactionSupport());
    }

    @Test
    void testFailsUnqualifiedKievServicesWhenSingleDataStoreConfigured() {
        configureRegistry(singleDataStoreConfig());

        ServiceRegistryException ex = assertThrows(ServiceRegistryException.class,
                                                   () -> Services.get(UnqualifiedKievInjectionService.class));

        assertEquals("Kiev DataStore injection requires @Service.Named to select a configured data store. "
                             + "Registered store names: primary-store. Add @Service.Named with one of these names.",
                     rootCause(ex).getMessage());
    }

    @Test
    void testFailsUnqualifiedKievServicesWhenMultipleDataStoresConfigured() {
        configureRegistry(multipleDataStoresConfig());

        ServiceRegistryException ex = assertThrows(ServiceRegistryException.class,
                                                   () -> Services.get(UnqualifiedKievInjectionService.class));

        assertEquals("Kiev DataStore injection requires @Service.Named to select a configured data store. "
                             + "Registered store names: primary-store, secondary-store. "
                             + "Add @Service.Named with one of these names.",
                     rootCause(ex).getMessage());
    }

    private void configureRegistry(Map<String, String> values) {
        registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());
        Services.set(Config.class, Config.just(ConfigSources.create(values)));
    }

    private static Map<String, String> singleDataStoreConfig() {
        return Map.of(
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app"
        );
    }

    private static Map<String, String> multipleDataStoresConfig() {
        return Map.of(
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app",
                "oci.kiev.data-stores.1.store-name", "secondary-store",
                "oci.kiev.data-stores.1.app-name", "secondary-app"
        );
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}

@Service.Singleton
class NamedKievInjectionService {
    private final DataStore dataStore;
    private final MappedDataStore mappedDataStore;
    private final KievTransactionSupport transactionSupport;

    @Service.Inject
    NamedKievInjectionService(@Service.Named("secondary-store") DataStore dataStore,
                              @Service.Named("secondary-store") MappedDataStore mappedDataStore,
                              @Service.Named("secondary-store") KievTransactionSupport transactionSupport) {
        this.dataStore = dataStore;
        this.mappedDataStore = mappedDataStore;
        this.transactionSupport = transactionSupport;
    }

    DataStore dataStore() {
        return dataStore;
    }

    MappedDataStore mappedDataStore() {
        return mappedDataStore;
    }

    KievTransactionSupport transactionSupport() {
        return transactionSupport;
    }
}

@Service.Singleton
class UnqualifiedKievInjectionService {
    private final DataStore dataStore;
    private final MappedDataStore mappedDataStore;
    private final KievTransactionSupport transactionSupport;

    @Service.Inject
    UnqualifiedKievInjectionService(DataStore dataStore,
                                    MappedDataStore mappedDataStore,
                                    KievTransactionSupport transactionSupport) {
        this.dataStore = dataStore;
        this.mappedDataStore = mappedDataStore;
        this.transactionSupport = transactionSupport;
    }

    DataStore dataStore() {
        return dataStore;
    }

    MappedDataStore mappedDataStore() {
        return mappedDataStore;
    }

    KievTransactionSupport transactionSupport() {
        return transactionSupport;
    }
}
