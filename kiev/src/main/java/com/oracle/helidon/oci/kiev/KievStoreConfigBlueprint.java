/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for a Kiev data store.
 */
@Prototype.Blueprint
@Prototype.Configured
interface KievStoreConfigBlueprint {

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

    /**
     * Whether delete stream records should include deleted column values.
     *
     * @return whether deleted column values should be included
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean streamDeletedColumnValues();
}
