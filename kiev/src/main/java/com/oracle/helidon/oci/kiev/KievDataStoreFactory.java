/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.List;
import java.util.Optional;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Lookup;
import io.helidon.service.registry.Qualifier;
import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.DataStore;

/**
 * Factory that exposes configured Kiev data stores as named services.
 */
@Service.Singleton
@Service.Named(Service.Named.WILDCARD_NAME)
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class KievDataStoreFactory implements Service.InjectionPointFactory<DataStore> {
    private final KievDataStores dataStores;

    @Service.Inject
    KievDataStoreFactory(KievDataStores dataStores) {
        this.dataStores = dataStores;
    }

    @Override
    public Optional<Service.QualifiedInstance<DataStore>> first(Lookup lookup) {
        return list(lookup).stream().findFirst();
    }

    @Override
    public List<Service.QualifiedInstance<DataStore>> list(Lookup lookup) {
        Optional<String> storeName = KievDataStoreLookup.storeName(lookup);
        if (storeName.isPresent()) {
            String selectedStoreName = storeName.get();
            if (Service.Named.WILDCARD_NAME.equals(selectedStoreName)) {
                return allDataStores();
            }
            return List.of(dataStore(selectedStoreName));
        }

        throw dataStores.unqualifiedInjectionException("DataStore");
    }

    private List<Service.QualifiedInstance<DataStore>> allDataStores() {
        return dataStores.storeNames()
                .stream()
                .map(this::dataStore)
                .toList();
    }

    private Service.QualifiedInstance<DataStore> dataStore(String storeName) {
        return Service.QualifiedInstance.create(dataStores.dataStore(storeName),
                                                Qualifier.createNamed(storeName));
    }
}
