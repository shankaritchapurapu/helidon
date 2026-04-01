/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Root configuration for Helidon Kiev integration.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.kiev")
interface KievConfigBlueprint {

    /**
     * Selected Kiev backend.
     *
     * @return backend type
     */
    @Option.Configured
    @Option.Default("IN_MEMORY")
    KievBackend backend();

    /**
     * Kiev store name.
     *
     * @return store name
     */
    @Option.Configured
    String storeName();

    /**
     * Kiev application name.
     *
     * @return application name
     */
    @Option.Configured
    String appName();

    /**
     * Maximum reads per transaction.
     *
     * @return transaction read limit
     */
    @Option.Configured
    @Option.DefaultInt(100)
    int transactionMaxReads();

    /**
     * Maximum writes per transaction.
     *
     * @return transaction write limit
     */
    @Option.Configured
    @Option.DefaultInt(100)
    int transactionMaxWrites();

    /**
     * Direct DB backend configuration.
     *
     * @return optional direct DB configuration
     */
    @Option.Configured
    Optional<KievDirectDbConfig> directDb();

    /**
     * Service backend configuration.
     *
     * @return optional service configuration
     */
    @Option.Configured
    Optional<KievServiceConfig> service();
}
