/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class SecretServiceConfigSourceFactoryTest {
    @Test
    void readsOciConfigWhenMetaConfigMissing() {
        SecretServiceConfigSourceBuilder builder = new SecretServiceConfigSourceFactory(Optional.empty(),
                                                                                        Optional::empty)
                .builder();

        SecretServiceConfigSourceConfig sourceConfig = builder.sourceConfig();

        assertThat(sourceConfig.prefix(), is("oci.file.ssv2"));
        assertThat(sourceConfig.cacheTtl(), is(Duration.ofMinutes(5)));
        assertThat(sourceConfig.pollInterval().orElseThrow(), is(Duration.ofSeconds(15)));
        assertThat(builder.clientConfig().enabled(), is(false));
        assertThat(builder.clientConfig().endpoint(),
                   is("https://secret-service-ce.${oci.env.iaas-domain-name}/v1"));
        assertThat(builder.clientConfig().tlsConfig().caBundle(),
                   is("/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"));
    }

    @Test
    void systemPropertiesOverrideOciConfig() {
        withSystemProperties(Map.of(
                "helidon.oci-secret-service.prefix", "oci.system.ssv2",
                "helidon.oci-secret-service.cache-ttl", "PT5S",
                "helidon.oci-secret-service.client.endpoint", "https://override.example.com/v1"),
                             () -> {
                                 SecretServiceConfigSourceBuilder builder =
                                         new SecretServiceConfigSourceFactory(Optional.empty(), Optional::empty)
                                                 .builder();

                                 assertThat(builder.sourceConfig().prefix(), is("oci.system.ssv2"));
                                 assertThat(builder.sourceConfig().cacheTtl(), is(Duration.ofSeconds(5)));
                                 assertThat(builder.clientConfig().endpoint(), is("https://override.example.com/v1"));
                             });
    }

    @Test
    void providerConfigUsesClasspathOciConfigForMissingProperties() {
        // Empty provider properties exercise the fallback to helidon.oci-secret-service in oci-config.yaml.
        SecretServiceConfigSourceBuilder builder = SecretServiceConfigSourceFactory.builder(
                SecretServiceConfigSourceFactory.providerConfig(Config.empty()),
                Optional::empty);

        SecretServiceConfigSourceConfig sourceConfig = builder.sourceConfig();

        assertThat(sourceConfig.prefix(), is("oci.file.ssv2"));
        assertThat(sourceConfig.cacheTtl(), is(Duration.ofMinutes(5)));
        assertThat(sourceConfig.pollInterval().orElseThrow(), is(Duration.ofSeconds(15)));
        assertThat(builder.clientConfig().enabled(), is(false));
        assertThat(builder.clientConfig().endpoint(),
                   is("https://secret-service-ce.${oci.env.iaas-domain-name}/v1"));
        assertThat(builder.clientConfig().tlsConfig().caBundle(),
                   is("/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"));
    }

    @Test
    void providerConfigOverridesOciConfigPerKey() {
        // Provider properties are first in the merged config, so they override oci-config.yaml per key.
        Config metaConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("prefix", "oci.provider.ssv2"),
                entry("client.enabled", "true"),
                entry("client.endpoint", "https://provider.example.com/v1"),
                entry("client.retry-config.max-retries", "7"))));

        SecretServiceConfigSourceBuilder builder = SecretServiceConfigSourceFactory.builder(
                SecretServiceConfigSourceFactory.providerConfig(metaConfig),
                Optional::empty);

        SecretServiceConfigSourceConfig sourceConfig = builder.sourceConfig();

        assertThat(sourceConfig.prefix(), is("oci.provider.ssv2"));
        assertThat(sourceConfig.cacheTtl(), is(Duration.ofMinutes(5)));
        assertThat(sourceConfig.pollInterval().orElseThrow(), is(Duration.ofSeconds(15)));
        assertThat(builder.clientConfig().enabled(), is(true));
        assertThat(builder.clientConfig().endpoint(), is("https://provider.example.com/v1"));
        assertThat(builder.clientConfig().tlsConfig().caBundle(),
                   is("/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"));
        assertThat(builder.clientConfig().retryConfig().maxRetries(), is(7));
    }

    @Test
    void systemPropertyEndpointReferenceIsResolvedByClientConfig() {
        withSystemProperties(Map.of(
                "helidon.oci-secret-service.client.enabled", "true",
                "helidon.oci-secret-service.client.endpoint",
                "https://override.${oci.env.iaas-domain-name}/v1"),
                             () -> {
                                 ConfigSource ociEnvSource =
                                         ConfigSources.create(Map.of("oci.env.iaas-domain-name", "r1.oracleiaas.com"),
                                                              "oci-env")
                                                 .build();
                                 SecretServiceConfigSourceBuilder builder =
                                         new SecretServiceConfigSourceFactory(Optional.empty(), () -> Optional.of(ociEnvSource))
                                                 .builder();

                                 assertThat(builder.clientConfig().endpoint(),
                                            is("https://override.${oci.env.iaas-domain-name}/v1"));
                                 assertThat(builder.resolvedClientConfig().endpoint(),
                                            is("https://override.r1.oracleiaas.com/v1"));
                             });
    }

    @Test
    void usesDefaultsWhenOciConfigMissing() {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(new ResourceFilteringClassLoader(original, Set.of("oci-config.")));
        try {
            SecretServiceConfigSourceBuilder builder = new SecretServiceConfigSourceFactory(Optional.empty(),
                                                                                            Optional::empty)
                    .builder();

            assertThat(builder.sourceConfig().prefix(), is("oci.ssv2"));
            assertThat(builder.sourceConfig().cacheTtl(), is(Duration.ofMinutes(5)));
            assertThat(builder.sourceConfig().effectivePollInterval(), is(Duration.ofMinutes(30)));
            assertThat(builder.clientConfig().endpoint(), is(DefaultSsv2Client.DEFAULT_ENDPOINT));
            assertThat(builder.clientConfig().tlsConfig().caBundle(), is(DefaultSsv2Client.DEFAULT_CA_BUNDLE));
            assertThat(builder.clientConfig().retryConfig().maxRetries(), is(DefaultSsv2Client.DEFAULT_MAX_RETRIES));
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    void providerConfigUsesDefaultsWhenProviderAndOciConfigMissingProperties() {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(new ResourceFilteringClassLoader(original, Set.of("oci-config.")));
        try {
            SecretServiceConfigSourceBuilder builder = SecretServiceConfigSourceFactory.builder(
                    SecretServiceConfigSourceFactory.providerConfig(Config.empty()),
                    Optional::empty);

            assertThat(builder.sourceConfig().prefix(), is("oci.ssv2"));
            assertThat(builder.sourceConfig().cacheTtl(), is(Duration.ofMinutes(5)));
            assertThat(builder.sourceConfig().effectivePollInterval(), is(Duration.ofMinutes(30)));
            assertThat(builder.clientConfig().endpoint(), is(DefaultSsv2Client.DEFAULT_ENDPOINT));
            assertThat(builder.clientConfig().tlsConfig().caBundle(), is(DefaultSsv2Client.DEFAULT_CA_BUNDLE));
            assertThat(builder.clientConfig().retryConfig().maxRetries(), is(DefaultSsv2Client.DEFAULT_MAX_RETRIES));
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    void defaultEndpointReferenceIsResolvedByClientConfig() {
        withSystemProperties(Map.of("helidon.oci-secret-service.client.enabled", "true"),
                             () -> {
                                 ConfigSource ociEnvSource =
                                         ConfigSources.create(Map.of("oci.env.iaas-domain-name", "r2.oracleiaas.com"),
                                                              "oci-env")
                                                 .build();
                                 SecretServiceConfigSourceBuilder builder =
                                         new SecretServiceConfigSourceFactory(Optional.empty(), () -> Optional.of(ociEnvSource))
                                                 .builder();

                                 assertThat(builder.clientConfig().endpoint(), is(DefaultSsv2Client.DEFAULT_ENDPOINT));
                                 assertThat(builder.resolvedClientConfig().endpoint(),
                                            is("https://secret-service-ce.r2.oracleiaas.com/v1"));
                             });
    }

    @Test
    void concreteEndpointDoesNotResolveOciEnvSource() {
        withSystemProperties(Map.of(
                "helidon.oci-secret-service.client.enabled", "true",
                "helidon.oci-secret-service.client.endpoint", "https://secret-service.example.com/v1"),
                             () -> {
                                 SecretServiceConfigSourceBuilder builder =
                                         new SecretServiceConfigSourceFactory(Optional.empty(),
                                                                              SecretServiceConfigSourceFactoryTest::failOciEnvLookup)
                                                 .builder();

                                 assertThat(builder.resolvedClientConfig().endpoint(),
                                            is("https://secret-service.example.com/v1"));
                             });
    }

    @Test
    void escapedEndpointReferenceDoesNotResolveOciEnvSource() {
        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .disableValueResolving()
                .addSource(ConfigSources.create(Map.of(
                        "client.enabled", "true",
                        "client.endpoint", "https://secret-service.\\${oci.env.iaas-domain-name}/v1"))
                                   .build())
                .build();
        SecretServiceConfigSourceBuilder builder = SecretServiceConfigSource.builder()
                .config(config)
                .ociEnvConfigSource(SecretServiceConfigSourceFactoryTest::failOciEnvLookup);

        assertThat(builder.resolvedClientConfig().endpoint(),
                   is("https://secret-service.${oci.env.iaas-domain-name}/v1"));
    }

    @Test
    void disabledClientDoesNotResolveOciEnvSource() {
        SecretServiceConfigSourceBuilder builder =
                new SecretServiceConfigSourceFactory(Optional.empty(),
                                                     SecretServiceConfigSourceFactoryTest::failOciEnvLookup)
                        .builder();

        Ssv2ClientConfig clientConfig = builder.resolvedClientConfig();

        assertThat(clientConfig.enabled(), is(false));
        assertThat(clientConfig.endpoint(), is(DefaultSsv2Client.DEFAULT_ENDPOINT));
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

    private static Optional<ConfigSource> failOciEnvLookup() {
        throw new AssertionError("oci-env source should not be resolved");
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
        public Enumeration<URL> getResources(String name) throws java.io.IOException {
            if (filteredPrefixes.stream().anyMatch(name::startsWith)) {
                return Collections.emptyEnumeration();
            }
            return super.getResources(name);
        }
    }
}
