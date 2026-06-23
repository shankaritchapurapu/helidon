/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

/**
 * Kiev stream cursor snapshot.
 *
 * @param oldest oldest available cursor
 * @param newest newest available cursor
 */
record StoreStreamCursors(String oldest, String newest) {
}
