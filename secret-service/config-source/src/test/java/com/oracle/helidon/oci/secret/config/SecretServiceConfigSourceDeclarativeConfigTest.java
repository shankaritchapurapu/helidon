/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigSource;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class SecretServiceConfigSourceDeclarativeConfigTest {
    private static final SourceValues CLASSPATH_VALUES = new SourceValues(
            "oci.file.ssv2",
            Duration.ofMinutes(5),
            Duration.ofSeconds(15),
            "false",
            "https://secret-service-ce.${oci.env.iaas-domain-name}/v1",
            "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem",
            "3");
    private static final SourceValues FILESYSTEM_VALUES = new SourceValues(
            "oci.fs.ssv2",
            Duration.ofSeconds(20),
            Duration.ofSeconds(10),
            "false",
            "https://fs.example.com/v1",
            "/fs/ca-bundle.pem",
            "4");
    private static final SourceValues SYSTEM_VALUES = new SourceValues(
            "oci.system.ssv2",
            Duration.ofSeconds(5),
            Duration.ofSeconds(6),
            "true",
            "https://system.example.com/v1",
            "/system/ca-bundle.pem",
            "9");
    private static final SourceValues ENV_VALUES = new SourceValues(
            "oci.env.ssv2",
            Duration.ofSeconds(2),
            Duration.ofSeconds(3),
            "false",
            "https://env.example.com/v1",
            "/env/ca-bundle.pem",
            "11");
    private static final SourceValues DEFAULT_VALUES = new SourceValues(
            "oci.ssv2",
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            "true",
            Ssv2Client.DEFAULT_ENDPOINT,
            Ssv2Client.DEFAULT_CA_BUNDLE,
            String.valueOf(Ssv2Client.DEFAULT_MAX_RETRIES));

    private ServiceRegistryManager registryManager;
    @TempDir
    private Path directory;

    @AfterEach
    void shutdownServices() {
        if (registryManager != null) {
            registryManager.shutdown();
        }
    }

    @Test
    void loadsSecretServiceSourceDeclarativelyWhenMetaConfigMissing() {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(new ResourceFilteringClassLoader(original,
                                                                      Set.of("meta-config.", "config-profile.")));
        try {
            registryManager = ServiceRegistryManager.create();
            GlobalServiceRegistry.registry(registryManager.registry());

            ConfigSource source = Services.firstNamed(ConfigSource.class, SecretServiceConfigSource.TYPE)
                    .orElseThrow();

            assertThat(source, instanceOf(SecretServiceConfigSource.class));
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @TestFactory
    Stream<DynamicTest> loadsSettingsFromOciConfigFallbackForAllSourceCombinations() {
        return IntStream.range(0, 1 << SourceKind.values().length)
                .mapToObj(mask -> DynamicTest.dynamicTest(sourceCombinationName(mask), () -> {
                    SecretServiceConfigSourceBuilder builder = builderFromMask(mask);

                    assertSourceValues(builder, expectedValues(mask));
                }));
    }

    private SecretServiceConfigSourceBuilder builderFromMask(int mask) throws IOException {
        // Use deterministic test sources instead of the real process environment.
        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(sourceFor(mask, SourceKind.ENVIRONMENT))
                .addSource(sourceFor(mask, SourceKind.SYSTEM_PROPERTIES))
                .addSource(sourceFor(mask, SourceKind.FILESYSTEM))
                .addSource(sourceFor(mask, SourceKind.CLASSPATH))
                .build();
        return SecretServiceConfigSource.builder()
                .config(config.get("helidon.oci-secret-service"));
    }

    private ConfigSource sourceFor(int mask, SourceKind sourceKind) throws IOException {
        if (!sourceKind.included(mask)) {
            return source(sourceKind.sourceName(), null);
        }

        return switch (sourceKind) {
        case ENVIRONMENT, SYSTEM_PROPERTIES -> source(sourceKind.sourceName(), sourceKind.sourceValues());
        case FILESYSTEM -> {
            Path caseDirectory = directory.resolve("sources-" + mask);
            Files.createDirectories(caseDirectory);
            yield ConfigSources.file(writeOciConfig(caseDirectory, FILESYSTEM_VALUES)).build();
        }
        case CLASSPATH -> ConfigSources.classpath("oci-config.yaml").build();
        };
    }

    private static ConfigSource source(String name, SourceValues values) {
        return ConfigSources.create(values == null ? Map.of() : properties(values), name).build();
    }

    private static void assertSourceValues(SecretServiceConfigSourceBuilder builder, SourceValues expected) {
        SecretServiceConfigSourceConfig sourceConfig = builder.sourceConfig();
        Ssv2ClientConfig clientConfig = builder.clientConfig();

        assertThat(sourceConfig.prefix(), is(expected.prefix()));
        assertThat(sourceConfig.cacheTtl(), is(expected.cacheTtl()));
        assertThat(sourceConfig.pollInterval().orElse(sourceConfig.effectivePollInterval()), is(expected.pollInterval()));
        assertThat(sourceConfig.effectivePollInterval(), is(expected.pollInterval()));
        assertThat(String.valueOf(clientConfig.enabled()), is(expected.clientEnabled()));
        assertThat(clientConfig.endpoint(), is(expected.endpoint()));
        assertThat(clientConfig.tlsConfig().caBundle(), is(expected.caBundle()));
        assertThat(String.valueOf(clientConfig.retryConfig().maxRetries()), is(expected.maxRetries()));
    }

    private static Map<String, String> properties(SourceValues values) {
        return Map.of(
                "helidon.oci-secret-service.prefix", values.prefix(),
                "helidon.oci-secret-service.cache-ttl", values.cacheTtl().toString(),
                "helidon.oci-secret-service.poll-interval", values.pollInterval().toString(),
                "helidon.oci-secret-service.client.enabled", values.clientEnabled(),
                "helidon.oci-secret-service.client.endpoint", values.endpoint(),
                "helidon.oci-secret-service.client.tls-config.ca-bundle", values.caBundle(),
                "helidon.oci-secret-service.client.retry-config.max-retries", values.maxRetries());
    }

    private static Path writeOciConfig(Path directory, SourceValues values) throws IOException {
        Path path = directory.resolve("oci-config.yaml");
        Files.writeString(path,
                          """
                                  helidon:
                                    oci-secret-service:
                                      prefix: "%s"
                                      cache-ttl: "%s"
                                      poll-interval: "%s"
                                      client:
                                        enabled: %s
                                        endpoint: "%s"
                                        tls-config:
                                          ca-bundle: "%s"
                                        retry-config:
                                          max-retries: %s
                                  """.formatted(values.prefix(),
                                                values.cacheTtl(),
                                                values.pollInterval(),
                                                values.clientEnabled(),
                                                values.endpoint(),
                                                values.caBundle(),
                                                values.maxRetries()),
                          StandardCharsets.UTF_8);
        return path;
    }

    private static SourceValues expectedValues(int mask) {
        for (SourceKind sourceKind : SourceKind.values()) {
            if (sourceKind.included(mask)) {
                return sourceKind.sourceValues();
            }
        }
        return DEFAULT_VALUES;
    }

    private static String sourceCombinationName(int mask) {
        if (mask == 0) {
            return "missing meta-config and missing oci-config sources";
        }

        StringBuilder name = new StringBuilder("missing meta-config with ");
        String delimiter = "";
        for (SourceKind sourceKind : SourceKind.values()) {
            if (sourceKind.included(mask)) {
                name.append(delimiter)
                        .append(sourceKind.displayName());
                delimiter = " + ";
            }
        }
        return name.toString();
    }

    private record SourceValues(String prefix,
                                Duration cacheTtl,
                                Duration pollInterval,
                                String clientEnabled,
                                String endpoint,
                                String caBundle,
                                String maxRetries) {
    }

    private enum SourceKind {
        ENVIRONMENT(0b1000, "environment variables", "environment", ENV_VALUES),
        SYSTEM_PROPERTIES(0b0100, "system properties", "system", SYSTEM_VALUES),
        FILESYSTEM(0b0010, "filesystem oci-config.yaml", "file", FILESYSTEM_VALUES),
        CLASSPATH(0b0001, "classpath oci-config.yaml", "classpath", CLASSPATH_VALUES);

        private final int bit;
        private final String displayName;
        private final String sourceName;
        private final SourceValues values;

        SourceKind(int bit, String displayName, String sourceName, SourceValues values) {
            this.bit = bit;
            this.displayName = displayName;
            this.sourceName = sourceName;
            this.values = values;
        }

        boolean included(int mask) {
            return (mask & bit) != 0;
        }

        String displayName() {
            return displayName;
        }

        String sourceName() {
            return sourceName;
        }

        SourceValues sourceValues() {
            return values;
        }
    }

    private static final class ResourceFilteringClassLoader extends ClassLoader {
        private final Set<String> filteredPrefixes;

        private ResourceFilteringClassLoader(ClassLoader parent, Set<String> filteredPrefixes) {
            super(parent);
            this.filteredPrefixes = Set.copyOf(filteredPrefixes);
        }

        @Override
        public URL getResource(String name) {
            if (filteredPrefixes.stream().anyMatch(name::startsWith)) {
                return null;
            }
            return super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (filteredPrefixes.stream().anyMatch(name::startsWith)) {
                return Collections.emptyEnumeration();
            }
            return super.getResources(name);
        }
    }
}
