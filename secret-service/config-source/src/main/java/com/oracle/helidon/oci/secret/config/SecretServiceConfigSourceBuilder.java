/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.config.AbstractConfigSourceBuilder;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigSource;

/**
 * Builder for {@link SecretServiceConfigSource}.
 */
final class SecretServiceConfigSourceBuilder extends AbstractConfigSourceBuilder<SecretServiceConfigSourceBuilder, Void>
        implements io.helidon.common.Builder<SecretServiceConfigSourceBuilder, SecretServiceConfigSource> {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final SecretServiceConfigSourceConfig.Builder sourceConfig = SecretServiceConfigSourceConfig.builder();
    private Supplier<Optional<ConfigSource>> ociEnvConfigSource = Optional::empty;
    private Function<String, Optional<String>> resolver;
    private AutoCloseable resolverCloseable;
    private Clock clock = Clock.systemUTC();
    private ScheduledExecutorService scheduler;

    SecretServiceConfigSourceBuilder() {
    }

    /**
     * Set config key prefix used by this source.
     *
     * @param prefix config key prefix
     * @return updated builder
     */
    public SecretServiceConfigSourceBuilder prefix(String prefix) {
        sourceConfig.prefix(prefix);
        return this;
    }

    /**
     * Set source-level cache TTL for individual secret values.
     *
     * @param cacheTtl cache TTL
     * @return updated builder
     */
    public SecretServiceConfigSourceBuilder cacheTtl(Duration cacheTtl) {
        sourceConfig.cacheTtl(cacheTtl);
        return this;
    }

    /**
     * Set background poll interval used to detect external secret changes.
     *
     * @param pollInterval poll interval
     * @return updated builder
     */
    public SecretServiceConfigSourceBuilder pollInterval(Duration pollInterval) {
        sourceConfig.pollInterval(pollInterval);
        return this;
    }

    /**
     * Set SSv2 client configuration.
     *
     * @param clientConfig client config
     * @return updated builder
     */
    public SecretServiceConfigSourceBuilder clientConfig(Config clientConfig) {
        sourceConfig.client(Ssv2ClientConfig.create(Objects.requireNonNullElse(clientConfig, Config.empty())));
        return this;
    }

    /**
     * Configure the source from Helidon meta-config.
     * Use either a nested {@code client} subtree or root-level SSv2 client keys.
     * If both are used, values from the {@code client} subtree override duplicate root-level client keys.
     *
     * @param metaConfig source meta-config
     * @return updated builder
     */
    @Override
    public SecretServiceConfigSourceBuilder config(Config metaConfig) {
        Objects.requireNonNull(metaConfig, "metaConfig");
        if (metaConfig.get("polling-strategy").asNode().isPresent()) {
            throw new ConfigException("Invalid meta-configuration key: polling-strategy: "
                                              + "SecretServiceConfigSource uses internal SSv2 polling only");
        }
        if (metaConfig.get("change-watcher").asNode().isPresent()) {
            throw new ConfigException("Invalid meta-configuration key: change-watcher: "
                                              + "SecretServiceConfigSource uses internal SSv2 polling only");
        }

        super.config(metaConfig);

        sourceConfig.config(metaConfig);
        return this;
    }

    @Override
    public SecretServiceConfigSource build() {
        return new SecretServiceConfigSource(this);
    }

    SecretServiceConfigSourceBuilder resolver(Function<String, Optional<String>> resolver) {
        return resolver(resolver, null);
    }

    SecretServiceConfigSourceBuilder resolver(Function<String, Optional<String>> resolver, AutoCloseable closeable) {
        this.resolver = resolver;
        this.resolverCloseable = closeable;
        return this;
    }

    SecretServiceConfigSourceBuilder ociEnvConfigSource(Supplier<Optional<ConfigSource>> ociEnvConfigSource) {
        this.ociEnvConfigSource = Objects.requireNonNullElse(ociEnvConfigSource, Optional::empty);
        return this;
    }

    SecretServiceConfigSourceBuilder clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    SecretServiceConfigSourceBuilder scheduler(ScheduledExecutorService scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        return this;
    }

    Clock clock() {
        return clock;
    }

    SecretServiceConfigSourceConfig sourceConfig() {
        return sourceConfig.buildPrototype();
    }

    Ssv2ClientConfig clientConfig() {
        return sourceConfig().client();
    }

    Ssv2ClientConfig resolvedClientConfig() {
        Ssv2ClientConfig clientConfig = clientConfig();
        if (!clientConfig.enabled()) {
            return clientConfig;
        }

        String endpoint = clientConfig.endpoint();
        try {
            endpoint = resolveEndpoint(endpoint, Optional.empty());
        } catch (ConfigException e) {
            Optional<ConfigSource> ociEnvSource = ociEnvConfigSource.get();
            if (ociEnvSource.isEmpty()) {
                throw e;
            }
            endpoint = resolveEndpoint(endpoint, ociEnvSource);
        }
        return resolvedEndpoint(clientConfig, endpoint);
    }

    private static String resolveEndpoint(String endpoint, Optional<ConfigSource> ociEnvSource) {
        Config.Builder endpointConfig = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .failOnMissingValueReference(true)
                .addSource(ConfigSources.create(Map.of("endpoint", endpoint)));
        ociEnvSource.ifPresent(endpointConfig::addSource);
        return endpointConfig.build()
                .get("endpoint")
                .asString()
                .orElseThrow();
    }

    private static Ssv2ClientConfig resolvedEndpoint(Ssv2ClientConfig clientConfig, String endpoint) {
        if (clientConfig.endpoint().equals(endpoint)) {
            return clientConfig;
        }
        return Ssv2ClientConfig.builder(clientConfig)
                .endpoint(endpoint)
                .buildPrototype();
    }

    boolean ownsScheduler() {
        return scheduler == null;
    }

    Function<String, Optional<String>> resolver() {
        if (resolver != null) {
            return resolver;
        }

        LazyValue<DefaultSsv2Client> client = LazyValue.create(() -> new DefaultSsv2Client(resolvedClientConfig()));
        this.resolverCloseable = () -> {
            if (client.isLoaded()) {
                client.get().close();
            }
        };
        return path -> client.get().getSecretAsString(path);
    }

    AutoCloseable resolverCloseable() {
        return resolverCloseable;
    }

    ScheduledExecutorService scheduler() {
        if (scheduler != null) {
            return scheduler;
        }

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable,
                                       "secret-service-config-poll-"
                                               + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }
}
