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

import com.oracle.pic.kiev.mapping.MappedDataStore;

/**
 * Factory that exposes configured Kiev mapped data stores as named services.
 */
@Service.Singleton
@Service.Named(Service.Named.WILDCARD_NAME)
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class KievMappedDataStoreFactory implements Service.InjectionPointFactory<MappedDataStore> {
    private final KievDataStores dataStores;

    @Service.Inject
    KievMappedDataStoreFactory(KievDataStores dataStores) {
        this.dataStores = dataStores;
    }

    @Override
    public Optional<Service.QualifiedInstance<MappedDataStore>> first(Lookup lookup) {
        return list(lookup).stream().findFirst();
    }

    @Override
    public List<Service.QualifiedInstance<MappedDataStore>> list(Lookup lookup) {
        Optional<String> storeName = KievDataStoreLookup.storeName(lookup);
        if (storeName.isPresent()) {
            String selectedStoreName = storeName.get();
            if (Service.Named.WILDCARD_NAME.equals(selectedStoreName)) {
                return allMappedDataStores();
            }
            return List.of(mappedDataStore(selectedStoreName));
        }

        throw dataStores.unqualifiedInjectionException("MappedDataStore");
    }

    private List<Service.QualifiedInstance<MappedDataStore>> allMappedDataStores() {
        return dataStores.storeNames()
                .stream()
                .map(this::mappedDataStore)
                .toList();
    }

    private Service.QualifiedInstance<MappedDataStore> mappedDataStore(String storeName) {
        return Service.QualifiedInstance.create(dataStores.mappedDataStore(storeName),
                                                Qualifier.createNamed(storeName));
    }
}
