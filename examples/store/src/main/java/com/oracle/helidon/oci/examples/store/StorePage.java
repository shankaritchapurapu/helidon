/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import java.util.List;
import java.util.Optional;

/**
 * Paged store results.
 *
 * @param items items in the current page
 * @param nextPageToken token to request the next page
 */
record StorePage(List<StoreItem> items, Optional<String> nextPageToken) {
}
