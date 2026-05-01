/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

/**
 * Supported Kiev service authentication types.
 */
public enum KievAuthType {
    /**
     * OCI instance principal authentication.
     */
    INSTANCE,
    /**
     * Use an externally provided OCI authentication provider.
     */
    OVERRIDDEN,
    /**
     * Service-to-service authentication.
     */
    S2S,
    /**
     * Local KIAB KaaS using offline/overridden auth.
     */
    KIAB_LOCAL
}
