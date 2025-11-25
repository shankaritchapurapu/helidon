/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import io.helidon.common.configurable.ResourceConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultSecretServiceTlsManagerTest {

    @Test
    void testMultipleCallsToTrustStream() throws Exception {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .pki(PkiConfig.builder().build())
                .reload(ReloadConfig.builder().build())
                .trust(ResourceConfig.builder().resourcePath("mtls/ca.pem").build())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);
        byte[] priorBytes = null;
        // check 5x
        for (int i = 0; i < 5; i++) {
            var currentBytes = manager.getTrustStream().readAllBytes();
            if (priorBytes != null) {
                assertThat(currentBytes, is(priorBytes));
            }
            priorBytes = currentBytes;
        }
    }
}
