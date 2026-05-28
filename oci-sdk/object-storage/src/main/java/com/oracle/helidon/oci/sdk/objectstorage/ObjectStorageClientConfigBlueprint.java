/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.objectstorage;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.objectstorage.ObjectStorageClient;

/**
 * Configuration used to create the native Object Storage client.
 */
@Prototype.Blueprint(decorator = ConfigSupport.ObjectStorageClientSupport.class)
@Prototype.Configured(ObjectStorageClientConfigFactory.OCI_OBJECT_STORAGE)
@Prototype.CustomMethods(ConfigSupport.ObjectStorageClientSupport.class)
interface ObjectStorageClientConfigBlueprint extends Prototype.Factory<ObjectStorageClient> {
    /**
     * Object Storage endpoint.
     *
     * @return endpoint
     */
    @Option.Configured
    Optional<String> endpoint();

    /**
     * Object Storage region.
     *
     * @return region
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Alias for {@link #region()} using the common OCI region ID name.
     *
     * @return region ID
     */
    @Option.Configured
    Optional<String> regionId();

    /**
     * OCI SDK client configuration.
     *
     * @return client configuration
     */
    @Option.Configured
    Optional<ClientConfiguration> client();
}
