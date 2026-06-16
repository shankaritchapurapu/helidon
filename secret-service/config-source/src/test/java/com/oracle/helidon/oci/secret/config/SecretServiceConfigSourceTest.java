/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigNode;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretServiceConfigSourceTest {
    @Test
    void loadsOnlyMatchingPrefixedKeysLazily() {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> {
                    requestedPath.set(path);
                    return Optional.of("lazy-value");
                })
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            assertThat(config.get("oci.ssv2/secret/demo").asString().orElseThrow(), is("lazy-value"));
            assertThat(config.get("other.prefix/secret/demo").exists(), is(false));
            assertThat(requestedPath.get(), is("/secret/demo"));
        }
    }

    @Test
    void respectsConfiguredCacheTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-28T10:00:00Z"));
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<String> value = new AtomicReference<>("v1");

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .clock(clock)
                .cacheTtl(Duration.ofSeconds(30))
                .resolver(path -> {
                    callCount.incrementAndGet();
                    return Optional.of(value.get());
                })
                .build();

        try (source) {
            assertThat(value(source.node("oci.ssv2/secret/cache")), is("v1"));
            value.set("v2");

            assertThat(value(source.node("oci.ssv2/secret/cache")), is("v1"));
            assertThat(callCount.get(), is(1));

            clock.advance(Duration.ofSeconds(31));
            assertThat(value(source.node("oci.ssv2/secret/cache")), is("v2"));
            assertThat(callCount.get(), is(2));
        }
    }

    @Test
    void returnsEmptyNodeForMissingInitialSecret() {
        AtomicInteger callCount = new AtomicInteger();
        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> {
                    callCount.incrementAndGet();
                    return Optional.empty();
                })
                .build();

        try (source) {
            assertThat(source.node("oci.ssv2/secret/missing").isPresent(), is(false));
            assertThat(source.node("oci.ssv2/secret/missing").isPresent(), is(false));
            assertThat(callCount.get(), is(1));
        }
    }

    @Test
    void backsOffInitialReadFailuresUntilCacheTtlExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-28T10:00:00Z"));
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<RuntimeException> failure = new AtomicReference<>(new IllegalStateException("boom"));

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .clock(clock)
                .cacheTtl(Duration.ofSeconds(30))
                .resolver(path -> {
                    callCount.incrementAndGet();
                    RuntimeException currentFailure = failure.get();
                    if (currentFailure != null) {
                        throw currentFailure;
                    }
                    return Optional.of("recovered");
                })
                .build();

        try (source) {
            Optional<ConfigNode> firstFailure = assertDoesNotThrow(() -> source.node("oci.ssv2/secret/failure"));
            Optional<ConfigNode> secondFailure = assertDoesNotThrow(() -> source.node("oci.ssv2/secret/failure"));

            assertThat(firstFailure.isPresent(), is(false));
            assertThat(secondFailure.isPresent(), is(false));
            assertThat(callCount.get(), is(1));

            failure.set(null);

            assertThat(source.node("oci.ssv2/secret/failure").isPresent(), is(false));
            assertThat(callCount.get(), is(1));

            clock.advance(Duration.ofSeconds(31));
            assertThat(value(source.node("oci.ssv2/secret/failure")), is("recovered"));
            assertThat(callCount.get(), is(2));
        }
    }

    @Test
    void initialReadFailureReturnsEmptyWhenConfigValueIsRequested() {
        AtomicInteger callCount = new AtomicInteger();
        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> {
                    callCount.incrementAndGet();
                    throw new IllegalStateException("boom");
                })
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            var value = assertDoesNotThrow(() -> config.get("oci.ssv2/secret/db/password").asString());

            assertThat(value.isPresent(), is(false));
            assertThat(callCount.get(), is(1));
        }
    }

    @Test
    void initialReadFailureDuringKnownKeyMergeReturnsLowerPriorityFallback() {
        AtomicInteger callCount = new AtomicInteger();
        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> {
                    callCount.incrementAndGet();
                    throw new ConfigException("boom");
                })
                .build();

        try (source) {
            Config config = assertDoesNotThrow(() -> Config.builder()
                    .disableEnvironmentVariablesSource()
                    .disableSystemPropertiesSource()
                    .addSource(source)
                    .addSource(ConfigSources.create(Map.of("oci.ssv2/secret/db/password", "fallback")))
                    .build());
            assertThat(callCount.get(), is(1));

            String value = assertDoesNotThrow(
                    () -> config.get("oci.ssv2/secret/db/password").asString().orElseThrow());

            assertThat(value, is("fallback"));
            assertThat(callCount.get(), is(1));
        }
    }

    @Test
    void keepsCachedValueWhenRefreshFails() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-28T10:00:00Z"));
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<String> value = new AtomicReference<>("v1");
        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .clock(clock)
                .cacheTtl(Duration.ofSeconds(30))
                .resolver(path -> {
                    callCount.incrementAndGet();
                    RuntimeException currentFailure = failure.get();
                    if (currentFailure != null) {
                        throw currentFailure;
                    }
                    return Optional.of(value.get());
                })
                .build();

        try (source) {
            assertThat(value(source.node("oci.ssv2/secret/refresh-failure")), is("v1"));

            value.set("v2");
            failure.set(new IllegalStateException("boom"));
            clock.advance(Duration.ofSeconds(31));

            assertThat(value(source.node("oci.ssv2/secret/refresh-failure")), is("v1"));
            assertThat(callCount.get(), is(2));

            failure.set(null);
            clock.advance(Duration.ofSeconds(31));

            assertThat(value(source.node("oci.ssv2/secret/refresh-failure")), is("v2"));
            assertThat(callCount.get(), is(3));
        }
    }

    @Test
    void publishesUpdatedSnapshotWhenTrackedSecretChanges() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> backingStore = new ConcurrentHashMap<>();
        backingStore.put("/secret/live", "v1");

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .cacheTtl(Duration.ofMinutes(5))
                .pollInterval(Duration.ofMillis(25))
                .resolver(path -> Optional.ofNullable(backingStore.get(path)))
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            var node = config.get("oci.ssv2/secret/live");
            assertThat(node.asString().orElseThrow(), is("v1"));

            AtomicReference<String> changedValue = new AtomicReference<>();
            node.onChange(updated -> {
                changedValue.set(updated.asString().orElse(null));
                latch.countDown();
            });

            backingStore.put("/secret/live", "v2");

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected config change notification");
            assertThat(changedValue.get(), is("v2"));
        }
    }

    @Test
    void publishesDeletedSnapshotWhenTrackedSecretDisappears() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> backingStore = new ConcurrentHashMap<>();
        backingStore.put("/secret/deleted", "v1");

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .cacheTtl(Duration.ofMinutes(5))
                .pollInterval(Duration.ofMillis(25))
                .resolver(path -> Optional.ofNullable(backingStore.get(path)))
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            var node = config.get("oci.ssv2/secret/deleted");
            assertThat(node.asString().orElseThrow(), is("v1"));

            AtomicReference<Boolean> changedExists = new AtomicReference<>();
            AtomicReference<String> changedValue = new AtomicReference<>("sentinel");
            node.onChange(updated -> {
                changedExists.set(updated.exists());
                changedValue.set(updated.asString().orElse(null));
                if (!updated.exists()) {
                    latch.countDown();
                }
            });

            backingStore.remove("/secret/deleted");

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected config change notification");
            assertThat(changedExists.get(), is(false));
            assertThat(changedValue.get(), is((String) null));
            assertThat(node.context().last().exists(), is(false));
        }
    }

    @Test
    void publishesUpdatedSnapshotWhenOnDemandReadRefreshesTrackedSecret() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> backingStore = new ConcurrentHashMap<>();
        backingStore.put("/secret/direct-refresh", "v1");

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .cacheTtl(Duration.ZERO)
                .pollInterval(Duration.ofHours(1))
                .resolver(path -> Optional.ofNullable(backingStore.get(path)))
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            var node = config.get("oci.ssv2/secret/direct-refresh");
            assertThat(node.asString().orElseThrow(), is("v1"));

            AtomicReference<String> changedValue = new AtomicReference<>();
            node.onChange(updated -> {
                changedValue.set(updated.asString().orElse(null));
                latch.countDown();
            });

            backingStore.put("/secret/direct-refresh", "v2");

            assertThat(value(source.node("oci.ssv2/secret/direct-refresh")), is("v2"));
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected config change notification");
            assertThat(changedValue.get(), is("v2"));
        }
    }

    @Test
    void serializesOnDemandPublicationOnSchedulerThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> backingStore = new ConcurrentHashMap<>();
        backingStore.put("/secret/serialized", "v1");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "secret-service-config-test-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .cacheTtl(Duration.ZERO)
                .pollInterval(Duration.ofHours(1))
                .scheduler(scheduler)
                .resolver(path -> Optional.ofNullable(backingStore.get(path)))
                .build();

        try (source) {
            AtomicReference<String> callbackThread = new AtomicReference<>();
            source.onChange((key, updated) -> {
                callbackThread.set(Thread.currentThread().getName());
                latch.countDown();
            });

            assertThat(value(source.node("oci.ssv2/secret/serialized")), is("v1"));
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected config change notification");
            assertThat(callbackThread.get(), is("secret-service-config-test-scheduler"));
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void publishesRecoveredValueAfterInitialReadFailure() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> value = new AtomicReference<>("recovered");
        AtomicReference<RuntimeException> failure = new AtomicReference<>(new IllegalStateException("boom"));

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .cacheTtl(Duration.ofMinutes(5))
                .pollInterval(Duration.ofMillis(25))
                .resolver(path -> {
                    RuntimeException currentFailure = failure.get();
                    if (currentFailure != null) {
                        throw currentFailure;
                    }
                    return Optional.of(value.get());
                })
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            var node = config.get("oci.ssv2/secret/recover");
            assertThat(node.asString().isPresent(), is(false));

            AtomicReference<String> changedValue = new AtomicReference<>();
            node.onChange(updated -> {
                changedValue.set(updated.asString().orElse(null));
                latch.countDown();
            });

            failure.set(null);

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected recovery change notification");
            assertThat(changedValue.get(), is("recovered"));
        }
    }

    @Test
    void supportsSubMillisecondPollIntervals() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> backingStore = new ConcurrentHashMap<>();
        backingStore.put("/secret/fast", "v1");

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .cacheTtl(Duration.ofMinutes(5))
                .pollInterval(Duration.ofNanos(500_000))
                .resolver(path -> Optional.ofNullable(backingStore.get(path)))
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            var node = config.get("oci.ssv2/secret/fast");
            assertThat(node.asString().orElseThrow(), is("v1"));

            AtomicReference<String> changedValue = new AtomicReference<>();
            node.onChange(updated -> {
                changedValue.set(updated.asString().orElse(null));
                latch.countDown();
            });

            backingStore.put("/secret/fast", "v2");

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected config change notification");
            assertThat(changedValue.get(), is("v2"));
        }
    }

    @Test
    void rejectsBareRootSecretPaths() {
        AtomicInteger callCount = new AtomicInteger();

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> {
                    callCount.incrementAndGet();
                    return Optional.of("unexpected");
                })
                .build();

        try (source) {
            assertThat(source.node("oci.ssv2/").isPresent(), is(false));
            assertThat(callCount.get(), is(0));
        }
    }

    @Test
    void builderRejectsNullScheduler() {
        assertThrows(NullPointerException.class, () -> SecretServiceConfigSource.builder().scheduler(null));
    }

    @Test
    void closeClosesResolverCloseable() {
        AtomicInteger closeCount = new AtomicInteger();

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> Optional.of("value"), closeCount::incrementAndGet)
                .build();

        source.close();
        source.close();

        assertThat(closeCount.get(), is(1));
    }

    @Test
    void builderConfigExtractsClientSettingsAndSourceSettings() {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        Config metaConfig = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(Map.of(
                        "prefix", "custom.ssv2",
                        "cache-ttl", "PT5S",
                        "poll-interval", "PT10S",
                        "client.endpoint", "https://secret-service-ce.${oci.env.iaas-domain-name}/v1",
                        "client.retry-config.max-retries", "9"
                )))
                .build();

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> {
                    requestedPath.set(path);
                    return Optional.of("meta-config-value");
                })
                .config(metaConfig)
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            assertThat(config.get("custom.ssv2/secret/test").asString().orElseThrow(), is("meta-config-value"));
            assertThat(config.get("oci.ssv2/secret/test").exists(), is(false));
            assertThat(requestedPath.get(), is("/secret/test"));
        }
    }

    @Test
    void builderConfigLoadsNestedClientSettings() {
        Config metaConfig = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(Map.of(
                        "client.enabled", "false",
                        "client.endpoint", "https://nested.${oci.env.iaas-domain-name}/v1",
                        "client.retry-config.max-retries", "9",
                        "client.tls-config.ca-bundle", "/tmp/test-ca.pem"
                )))
                .build();

        SecretServiceConfigSourceBuilder builder = SecretServiceConfigSource.builder()
                .config(metaConfig);

        assertThat(builder.clientConfig().enabled(), is(false));
        assertThat(builder.clientConfig().endpoint(), is("https://nested.${oci.env.iaas-domain-name}/v1"));
        assertThat(builder.clientConfig().retryConfig().maxRetries(), is(9));
        assertThat(builder.clientConfig().tlsConfig().caBundle(), is("/tmp/test-ca.pem"));
    }

    @Test
    void builderConfigUsesGeneratedValidationAndNormalization() {
        Config metaConfig = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(Map.of("prefix", " custom.ssv2///")))
                .build();

        SecretServiceConfigSourceBuilder builder = SecretServiceConfigSource.builder()
                .config(metaConfig);

        assertThat(builder.sourceConfig().prefix(), is("custom.ssv2"));

        Config invalidMetaConfig = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(Map.of("poll-interval", "PT0S")))
                .build();

        assertThrows(IllegalArgumentException.class,
                     () -> SecretServiceConfigSource.builder()
                             .config(invalidMetaConfig)
                             .build());
    }

    @Test
    void builderConfigUsesDefaultPollIntervalIndependentOfCacheTtl() {
        Config metaConfig = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(Map.of("cache-ttl", "PT5S")))
                .build();

        SecretServiceConfigSourceConfig sourceConfig = SecretServiceConfigSource.builder()
                .config(metaConfig)
                .sourceConfig();

        assertThat(sourceConfig.cacheTtl(), is(Duration.ofSeconds(5)));
        assertThat(sourceConfig.pollInterval().isEmpty(), is(true));
        assertThat(sourceConfig.effectivePollInterval(), is(Duration.ofMinutes(30)));
    }

    @Test
    void builderConfigUsesBlueprintPollInterval() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> backingStore = new ConcurrentHashMap<>();
        backingStore.put("/secret/poll", "v1");

        Config metaConfig = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(Map.of(
                        "cache-ttl", "PT5M",
                        "poll-interval", "PT0.025S"
                )))
                .build();

        SecretServiceConfigSource source = SecretServiceConfigSource.builder()
                .resolver(path -> Optional.ofNullable(backingStore.get(path)))
                .config(metaConfig)
                .build();

        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(source)
                .build();

        try (source) {
            var node = config.get("oci.ssv2/secret/poll");
            assertThat(node.asString().orElseThrow(), is("v1"));

            AtomicReference<String> changedValue = new AtomicReference<>();
            node.onChange(updated -> {
                changedValue.set(updated.asString().orElse(null));
                latch.countDown();
            });

            backingStore.put("/secret/poll", "v2");

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected config change notification");
            assertThat(changedValue.get(), is("v2"));
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;
        private final ZoneId zoneId;

        private MutableClock(Instant initialInstant) {
            this(initialInstant, ZoneOffset.UTC);
        }

        private MutableClock(Instant initialInstant, ZoneId zoneId) {
            this.current = new AtomicReference<>(initialInstant);
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current.get(), zone);
        }

        @Override
        public Instant instant() {
            return current.get();
        }

        private void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }
    }

    private static String value(Optional<ConfigNode> node) {
        return ((ConfigNode.ValueNode) node.orElseThrow()).get();
    }
}
