/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import com.oracle.oci.limits.LimitsDPClient;
import io.helidon.service.registry.Services;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LimitsDpClientFactoryTest {

    @Test
    void limitsDPClient() {
        LimitsDPClient client = Services.get(LimitsDPClient.class);
        assertNotNull(client, "LimitsDPClient instance should not be null");
    }

    @Test
    void config() {
        LimitsConfig config = Services.get(LimitsConfig.class);
        assertNotNull(config, "LimitsConfig instance should not be null");
        assertEquals(5000L, config.connectionTimeout().toMillis(), "Connection timeout should match");
        assertEquals(30000L, config.readTimeout().toMillis(), "Read timeout should match");
        assertEquals(20, config.maxAsyncThreads(), "Max async threads should match");
    }
}
