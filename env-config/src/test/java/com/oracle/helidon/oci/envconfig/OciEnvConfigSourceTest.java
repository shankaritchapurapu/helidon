/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigNode;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class OciEnvConfigSourceTest {

    @Test
    void createsEnvironmentValuesOnFirstAccessOnly() {
        AtomicInteger createCount = new AtomicInteger();
        OciEnvConfigSource source = new OciEnvConfigSource("test.env", () -> {
            createCount.incrementAndGet();
            return OciEnvConfigFactory.configNodes(ConfigNode.ObjectNode.builder()
                                                          .addValue("test.env.region", "us-ashburn-1")
                                                          .addValue("test.env.availability-domain", "iad-ad-1")
                                                          .build());
        });

        Config config = Config.builder(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        assertThat(createCount.get(), is(0));

        assertThat(config.get("test.env").get("region").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(createCount.get(), is(1));

        assertThat(config.get("test.env.availability-domain").asString().orElseThrow(), is("iad-ad-1"));
        assertThat(createCount.get(), is(1));
    }

    @Test
    void resolvesPlaceholdersWhenInterpolationRequestsLazyValue() {
        AtomicInteger createCount = new AtomicInteger();
        OciEnvConfigSource source = new OciEnvConfigSource("test.env", () -> {
            createCount.incrementAndGet();
            return OciEnvConfigFactory.configNodes(ConfigNode.ObjectNode.builder()
                                                          .addValue("test.env.ad-number", "ad1")
                                                          .build());
        });

        Config config = Config.builder()
                .addSource(ConfigSources.create(Map.of("endpoint", "https://service.${test.env.ad-number}.example.com")))
                .addSource(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        assertThat(createCount.get(), is(0));
        assertThat(config.get("endpoint").asString().orElseThrow(), is("https://service.ad1.example.com"));
        assertThat(createCount.get(), is(1));
    }

    @Test
    void ignoresUnrelatedKeysWithoutMaterializingValues() {
        AtomicInteger createCount = new AtomicInteger();
        OciEnvConfigSource source = new OciEnvConfigSource("test.env", () -> {
            createCount.incrementAndGet();
            return OciEnvConfigFactory.configNodes(ConfigNode.ObjectNode.builder()
                                                          .addValue("test.env.region", "us-ashburn-1")
                                                          .build());
        });

        Config config = Config.builder(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        assertThat(createCount.get(), is(0));
        assertThat(config.get("other.key").exists(), is(false));
        assertThat(createCount.get(), is(0));
    }

    @Test
    void returnsEmptyValuesWhenMaterializationFails() {
        AtomicInteger createCount = new AtomicInteger();
        OciEnvConfigSource source = new OciEnvConfigSource("test.env", () -> {
            createCount.incrementAndGet();
            throw new IllegalStateException("failed to resolve environment");
        });

        Config config = Config.builder(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        assertThat(createCount.get(), is(0));

        assertThat(config.get("test.env.region").exists(), is(false));
        assertThat(createCount.get(), is(1));

        assertThat(config.get("test.env.availability-domain").exists(), is(false));
        assertThat(createCount.get(), is(1));
    }

    @Test
    void loadsConfigFromOciConfigYamlWhenMetaConfigMissing() {
        OciEnvConfigSource source = new OciEnvConfigSource(Config.empty());

        Config config = Config.builder(source)
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        assertThat(config.get("oci.file.region").asString().orElseThrow(), is("us-ashburn-1"));
        assertThat(config.get("oci.file.availability-domain").asString().orElseThrow(), is("iad-ad-1"));
        assertThat(config.get("oci.file.fault-domain").asString().orElseThrow(), is("5"));
    }

    @Test
    void systemPropertiesOverrideOciConfigYamlWhenMetaConfigMissing() {
        withSystemProperties(Map.of(
                "helidon.oci-env.prefix", "oci.system",
                "helidon.oci-env.location-override.region", "eu-frankfurt-1",
                "helidon.oci-env.location-override.availability-domain", "eu-frankfurt-1-ad-1",
                "helidon.oci-env.location-override.fault-domain", "2"),
                             () -> {
                                 OciEnvConfigSource source = new OciEnvConfigSource(Config.empty());

                                 Config config = Config.builder(source)
                                         .disableEnvironmentVariablesSource()
                                         .disableSystemPropertiesSource()
                                         .build();

                                 assertThat(config.get("oci.system.region").asString().orElseThrow(), is("eu-frankfurt-1"));
                                 assertThat(config.get("oci.system.availability-domain").asString().orElseThrow(),
                                            is("eu-frankfurt-1-ad-1"));
                                 assertThat(config.get("oci.system.fault-domain").asString().orElseThrow(), is("2"));
                                 assertThat(config.get("oci.file.region").exists(), is(false));
                             });
    }

    private static void withSystemProperties(Map<String, String> properties, Runnable runnable) {
        Map<String, String> originalValues = new HashMap<>();
        Set<String> missingKeys = new HashSet<>();

        properties.forEach((key, value) -> {
            String originalValue = System.getProperty(key);
            if (originalValue == null) {
                missingKeys.add(key);
            } else {
                originalValues.put(key, originalValue);
            }
            System.setProperty(key, value);
        });

        try {
            runnable.run();
        } finally {
            missingKeys.forEach(System::clearProperty);
            originalValues.forEach(System::setProperty);
        }
    }
}
