/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration specific to a direct Oracle database Kiev connection.
 */
@Prototype.Blueprint
@Prototype.Configured
interface KievDirectDbConfigBlueprint {

    /**
     * JDBC URL for the Oracle database.
     *
     * @return JDBC URL
     */
    @Option.Configured
    String jdbcUrl();

    /**
     * Database user name.
     *
     * @return user name
     */
    @Option.Configured
    String userName();

    /**
     * Database password.
     *
     * @return password
     */
    @Option.Configured
    String password();

    /**
     * Optional schema name. Defaults to the database user when omitted.
     *
     * @return schema name
     */
    @Option.Configured
    Optional<String> schemaName();
}
