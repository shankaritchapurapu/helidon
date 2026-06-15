/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.objectstorage;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.pic.commons.util.Region;

/**
 * Factory that creates a configured OCI Java SDK {@link ObjectStorageClient}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class ObjectStorageClientFactory implements Supplier<ObjectStorageClient> {
    private final ObjectStorageClientConfig config;
    private final BasicAuthenticationDetailsProvider authProvider;
    private final Supplier<Region> regionSupplier;

    @Service.Inject
    ObjectStorageClientFactory(ObjectStorageClientConfig config,
                               BasicAuthenticationDetailsProvider authProvider,
                               Supplier<Region> regionSupplier) {
        this.config = config;
        this.authProvider = authProvider;
        this.regionSupplier = regionSupplier;
    }

    @Override
    public ObjectStorageClient get() {
        ObjectStorageClient.Builder builder = ObjectStorageClient.builder();
        config.client().ifPresent(builder::configuration);

        ObjectStorageClient client = builder.build(authProvider);
        configureEndpoint(config, client);
        // Throws exception if endpoint was not resolved
        client.getEndpoint();
        return client;
    }

    private void configureEndpoint(ObjectStorageClientConfig config, ObjectStorageClient client) {
        config.endpoint()
                .ifPresentOrElse(client::setEndpoint,
                                 () -> client.setRegion(RegionSupport.resolve(config.region(), regionSupplier)
                                                                 .getPublicRegionName()));
    }
}
