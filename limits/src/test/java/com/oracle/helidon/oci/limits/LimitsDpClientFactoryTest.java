/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import com.oracle.bmc.ConfigFileReader.ConfigFile;
import com.oracle.bmc.ClientConfiguration;
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
        ClientConfiguration client = config.client().orElseThrow();
        assertEquals(5000, client.getConnectionTimeoutMillis(), "Connection timeout should match");
        assertEquals(30000, client.getReadTimeoutMillis(), "Read timeout should match");
        assertEquals(20, client.getMaxAsyncThreads(), "Max async threads should match");
    }

    @Test
    void defaultClientConfigurationMatchesPreviousLimitsDefaults() {
        LimitsConfig config = LimitsConfig.builder().build();
        ClientConfiguration client = config.client()
                .orElseGet(() -> ClientConfiguration.builder().build());

        assertEquals(10000, client.getConnectionTimeoutMillis(), "Connection timeout should match");
        assertEquals(60000, client.getReadTimeoutMillis(), "Read timeout should match");
        assertEquals(50, client.getMaxAsyncThreads(), "Max async threads should match");
    }

    @Test
    void configFile() {
        ConfigFile configFile = Services.get(ConfigFile.class);
        assertNotNull(configFile, "ConfigFile instance should not be null");
        assertEquals("test", configFile.get("user"));
    }

}
