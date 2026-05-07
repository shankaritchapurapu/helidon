/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tagging;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaggingClientConfigFactoryTest {
    @Test
    void testDefaults() {
        Config config = Config.just(ConfigSources.create(Map.<String, String>of()));

        TaggingClientConfig taggingConfig = new TaggingClientConfigFactory(config).get();

        assertTrue(taggingConfig.emitMetrics());
    }

    @Test
    void testConfiguredEmitMetrics() {
        Config config = Config.just(ConfigSources.create(Map.of("oci.tagging.emit-metrics", "true")));

        TaggingClientConfig taggingConfig = new TaggingClientConfigFactory(config).get();

        assertTrue(taggingConfig.emitMetrics());
    }

    @Test
    void testConfiguredDisableMetrics() {
        Config config = Config.just(ConfigSources.create(Map.of("oci.tagging.emit-metrics", "false")));

        TaggingClientConfig taggingConfig = new TaggingClientConfigFactory(config).get();

        assertFalse(taggingConfig.emitMetrics());
    }
}
