/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

/**
 * Supported Kiev backends for the Helidon integration.
 */
public enum KievBackend {
    /**
     * Use the in-memory Kiev implementation.
     */
    IN_MEMORY,
    /**
     * Use Kiev through a direct Oracle database connection.
     */
    DIRECT_DB,
    /**
     * Use Kiev as a service.
     */
    SERVICE
}
