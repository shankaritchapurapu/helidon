/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.spi.ConfigSource;
import io.helidon.integrations.oci.spi.OciRegion;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.bmc.Region;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OciEnvConfigDeclarativeConfigTest {
    private ServiceRegistryManager registryManager;

    @BeforeAll
    static void beforeAll() {
        LogConfig.initClass();
    }

    @AfterEach
    void shutdownServices() {
        if (registryManager != null) {
            registryManager.shutdown();
        }
    }

    @Test
    void loadsOciEnvSourceFromDeclarativeConfig() {
        registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());

        Config config = Services.get(Config.class);

        // meta-config.yaml supplies the location while classpath oci-config.yaml supplies the prefix.
        assertThat(config.get("oci.file.region").asString().orElseThrow(), is("eu-frankfurt-1"));
        assertThat(config.get("oci.file.availability-domain").asString().orElseThrow(), is("eu-frankfurt-1-ad-1"));
        assertThat(config.get("oci.file.fault-domain").asString().orElseThrow(), is("2"));
        assertThat(config.get("oci.file.iaas-domain-name").asString().orElseThrow(),
                   is("eu-frankfurt-1.oracleiaas.com"));
    }

    @Test
    void loadsOciEnvSourceFromOciConfigWhenMetaConfigMissing() {
        withFilteredMetaConfig(() -> {
            registryManager = ServiceRegistryManager.create();
            GlobalServiceRegistry.registry(registryManager.registry());

            Config config = Services.get(Config.class);
            ConfigSource ociEnvSource = Services.firstNamed(ConfigSource.class, OciEnvConfigSourceProvider.TYPE)
                    .orElseThrow();

            // With meta-config filtered out, every configured value comes from classpath oci-config.yaml.
            assertThat(Services.firstNamed(ConfigSource.class, OciEnvConfigSourceProvider.TYPE).orElseThrow(),
                       sameInstance(ociEnvSource));
            assertThat(Services.all(ConfigSource.class)
                               .stream()
                               .anyMatch(source -> OciEnvConfigSource.DESCRIPTION.equals(source.description())),
                       is(true));
            assertThat(config.get("oci.file.region").asString().orElseThrow(), is("us-ashburn-1"));
            assertThat(config.get("oci.file.availability-domain").asString().orElseThrow(), is("iad-ad-1"));
            assertThat(config.get("oci.file.fault-domain").asString().orElseThrow(), is("5"));
        });
    }

    @Test
    void providesOciRegionWithHigherPriorityThanPublicHelidon() {
        withFilteredMetaConfig(() -> {
            registryManager = ServiceRegistryManager.create();
            GlobalServiceRegistry.registry(registryManager.registry());

            assertThat(Services.get(Region.class).getRegionId(), is("us-ashburn-1"));
            assertThat(lowerPriorityRegionProvider().region().map(Region::getRegionId).orElseThrow(),
                       is("eu-frankfurt-1"));
        });
    }

    @Test
    void providesPicRegionFromOciEnvConfiguration() {
        withFilteredMetaConfig(() -> {
            registryManager = ServiceRegistryManager.create();
            GlobalServiceRegistry.registry(registryManager.registry());

            com.oracle.pic.commons.util.Region region = Services.get(com.oracle.pic.commons.util.Region.class);

            assertThat(region.getPublicRegionName(), is("us-ashburn-1"));
        });
    }

    @Test
    void fallsBackToPublicRegionWhenOciEnvRegionUnavailable() {
        withSystemProperties(Map.of("helidon.oci.region", "eu-frankfurt-1"),
                             () -> withFilteredResources(() -> {
                                 registryManager = ServiceRegistryManager.create();
                                 GlobalServiceRegistry.registry(registryManager.registry());
                                 Services.set(OciEnvConfigFactory.class, factoryWithoutRegion());

                                 assertThat(ociEnvRegionProvider().region().isEmpty(), is(true));
                                 assertThat(Services.get(Region.class).getRegionId(), is("eu-frankfurt-1"));
                             },
                                                       "meta-config.",
                                                       "config-profile.",
                                                       "oci-config."));
    }

    @Test
    void doesNotFallBackToPublicRegionWhenOciEnvCannotResolveRegion() {
        withSystemProperties(Map.of("helidon.oci.region", "eu-frankfurt-1",
                                     "helidon.oci-env.dynamic-core-regions.enabled", "false",
                                     "helidon.oci-env.location-override.region", "not-a-region"),
                             () -> withFilteredResources(() -> {
                                 registryManager = ServiceRegistryManager.create();
                                 GlobalServiceRegistry.registry(registryManager.registry());

                                 ConfigException exception = assertThrows(ConfigException.class,
                                                                          () -> Services.get(Region.class));

                                 assertThat(exception.getMessage(),
                                            is("Configured location override region 'not-a-region' is invalid"));
                                 assertThat(lowerPriorityRegionProvider().region()
                                                                        .map(Region::getRegionId)
                                                                        .orElseThrow(),
                                            is("eu-frankfurt-1"));
                             },
                                                       "meta-config.",
                                                       "config-profile.",
                                                       "oci-config."));
    }

    private static OciRegion ociEnvRegionProvider() {
        return Services.all(OciRegion.class)
                .stream()
                .filter(OciEnvRegionImpl.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected oci-env OciRegion provider"));
    }

    private static OciRegion lowerPriorityRegionProvider() {
        return Services.all(OciRegion.class)
                .stream()
                .filter(regionProvider -> !(regionProvider instanceof OciEnvRegionImpl))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected lower-priority OciRegion provider"));
    }

    private static OciEnvConfigFactory factoryWithoutRegion() {
        return new OciEnvConfigFactory(Config.empty()) {
            @Override
            Optional<com.oracle.pic.commons.util.Region> region() {
                return Optional.empty();
            }
        };
    }

    private static void withFilteredMetaConfig(Runnable runnable) {
        withFilteredResources(runnable, "meta-config.", "config-profile.");
    }

    private static void withFilteredResources(Runnable runnable, String... filteredPrefixes) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(new ResourceFilteringClassLoader(original, Set.of(filteredPrefixes)));
        try {
            runnable.run();
        } finally {
            thread.setContextClassLoader(original);
        }
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

    private static final class ResourceFilteringClassLoader extends ClassLoader {
        private final Set<String> filteredPrefixes;

        private ResourceFilteringClassLoader(ClassLoader parent, Set<String> filteredPrefixes) {
            super(parent);
            this.filteredPrefixes = Set.copyOf(filteredPrefixes);
        }

        @Override
        public URL getResource(String name) {
            if (isFiltered(name)) {
                return null;
            }
            return super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (isFiltered(name)) {
                return Collections.emptyEnumeration();
            }
            return super.getResources(name);
        }

        private boolean isFiltered(String name) {
            return filteredPrefixes.stream().anyMatch(name::startsWith);
        }
    }
}
