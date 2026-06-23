/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

/**
 * Matching mode for OCI metrics publisher includes and excludes.
 */
public enum FilterMatchingMode {
    /**
     * Treat includes and excludes as exact metric names.
     */
    EXACT,

    /**
     * Treat includes and excludes as regular expressions.
     */
    REGEX,

    /**
     * Treat includes and excludes as metric name substrings.
     */
    SUBSTRING
}
