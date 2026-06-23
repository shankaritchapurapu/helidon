/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.List;
import java.util.Optional;

import io.helidon.service.registry.Lookup;
import io.helidon.service.registry.Qualifier;
import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.streams.service.client.core.Stream;

/**
 * Factory that exposes configured Kiev streaming clients as named services.
 */
@Service.Singleton
@Service.Named(Service.Named.WILDCARD_NAME)
class KievStreamFactory implements Service.InjectionPointFactory<Stream> {
    private final KievDataStores dataStores;

    @Service.Inject
    KievStreamFactory(KievDataStores dataStores) {
        this.dataStores = dataStores;
    }

    @Override
    public Optional<Service.QualifiedInstance<Stream>> first(Lookup lookup) {
        return list(lookup).stream().findFirst();
    }

    @Override
    public List<Service.QualifiedInstance<Stream>> list(Lookup lookup) {
        Optional<String> storeName = KievDataStoreLookup.storeName(lookup);
        if (storeName.isPresent()) {
            String selectedStoreName = storeName.get();
            if (Service.Named.WILDCARD_NAME.equals(selectedStoreName)) {
                return allStreams();
            }
            if (!dataStores.streamingStoreNames().contains(selectedStoreName)) {
                return List.of();
            }
            return List.of(stream(selectedStoreName));
        }

        throw dataStores.unqualifiedStreamingInjectionException();
    }

    private List<Service.QualifiedInstance<Stream>> allStreams() {
        return dataStores.streamingStoreNames()
                .stream()
                .map(this::stream)
                .toList();
    }

    private Service.QualifiedInstance<Stream> stream(String storeName) {
        return Service.QualifiedInstance.create(dataStores.stream(storeName),
                                                Qualifier.createNamed(storeName));
    }
}
