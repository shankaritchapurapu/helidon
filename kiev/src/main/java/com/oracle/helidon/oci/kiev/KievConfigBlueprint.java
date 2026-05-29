/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Root configuration for Helidon Kiev integration.
 */
@Prototype.Blueprint(decorator = KievConfigSupport.class)
@Prototype.Configured("oci.kiev")
interface KievConfigBlueprint {

    /**
     * Named Kiev data stores.
     *
     * @return configured data stores
     */
    @Option.Configured
    List<KievStoreConfig> dataStores();
}
