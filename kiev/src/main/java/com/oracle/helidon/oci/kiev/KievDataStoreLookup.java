/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.service.registry.Lookup;
import io.helidon.service.registry.Service;

final class KievDataStoreLookup {
    private KievDataStoreLookup() {
    }

    static Optional<String> storeName(Lookup lookup) {
        return lookup.qualifiers()
                .stream()
                .filter(qualifier -> Service.Named.TYPE.equals(qualifier.typeName()))
                .findFirst()
                .map(qualifier -> qualifier.value().orElse(Service.Named.WILDCARD_NAME));
    }
}
