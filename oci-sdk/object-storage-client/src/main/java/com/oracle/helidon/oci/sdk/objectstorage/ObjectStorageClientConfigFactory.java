/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.objectstorage;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory for {@link ObjectStorageClientConfig}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class ObjectStorageClientConfigFactory implements Supplier<ObjectStorageClientConfig> {

    static final String OCI_OBJECT_STORAGE = "oci.object-storage-client";
    private final Config config;

    @Service.Inject
    ObjectStorageClientConfigFactory(Config config) {
        this.config = config;
    }

    @Override
    public ObjectStorageClientConfig get() {
        return ObjectStorageClientConfig.create(config.get(OCI_OBJECT_STORAGE));
    }
}
