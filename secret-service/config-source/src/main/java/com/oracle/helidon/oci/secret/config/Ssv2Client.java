/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Optional;

/**
 * Client for reading secrets from Secret Service V2.
 */
public interface Ssv2Client {
    /**
     * Read and base64-decode a secret as bytes.
     *
     * @param path SSv2 secret path
     * @return decoded secret bytes, or empty when disabled or missing
     */
    Optional<byte[]> getSecretAsBytes(String path);
}
