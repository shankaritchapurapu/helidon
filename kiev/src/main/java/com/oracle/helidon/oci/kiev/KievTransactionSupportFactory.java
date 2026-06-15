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

/**
 * Factory that exposes configured Kiev transaction support services by store name.
 */
@Service.Singleton
@Service.Named(Service.Named.WILDCARD_NAME)
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class KievTransactionSupportFactory implements Service.InjectionPointFactory<KievTransactionSupport> {
    private final KievDataStores dataStores;

    @Service.Inject
    KievTransactionSupportFactory(KievDataStores dataStores) {
        this.dataStores = dataStores;
    }

    @Override
    public Optional<Service.QualifiedInstance<KievTransactionSupport>> first(Lookup lookup) {
        return list(lookup).stream().findFirst();
    }

    @Override
    public List<Service.QualifiedInstance<KievTransactionSupport>> list(Lookup lookup) {
        Optional<String> storeName = KievDataStoreLookup.storeName(lookup);
        if (storeName.isPresent()) {
            String selectedStoreName = storeName.get();
            if (Service.Named.WILDCARD_NAME.equals(selectedStoreName)) {
                return allTransactionSupports();
            }
            return List.of(transactionSupport(selectedStoreName));
        }

        throw dataStores.unqualifiedInjectionException("transaction support");
    }

    private List<Service.QualifiedInstance<KievTransactionSupport>> allTransactionSupports() {
        return dataStores.storeNames()
                .stream()
                .map(this::transactionSupport)
                .toList();
    }

    private Service.QualifiedInstance<KievTransactionSupport> transactionSupport(String storeName) {
        return Service.QualifiedInstance.create(dataStores.transactionSupport(storeName),
                                                Qualifier.createNamed(storeName));
    }
}
