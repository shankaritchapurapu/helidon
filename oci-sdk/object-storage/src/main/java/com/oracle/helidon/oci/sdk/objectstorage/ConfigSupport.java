/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.objectstorage;

import java.util.function.Function;

import io.helidon.builder.api.Prototype;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.EnumMapperProvider;
import io.helidon.service.registry.Services;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.retrier.DefaultRetryCondition;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.RetryOnOpenCircuitBreakerDefaultRetryCondition;
import com.oracle.bmc.retrier.RetryOptions;
import com.oracle.bmc.waiter.ExponentialBackoffDelayStrategy;
import com.oracle.bmc.waiter.ExponentialBackoffDelayStrategyWithJitter;
import com.oracle.bmc.waiter.FixedTimeDelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.MaxTimeTerminationStrategy;

/**
 * Runtime config helpers for OCI Object Storage client configuration blueprints.
 */
final class ConfigSupport {
    private ConfigSupport() {
    }

    static final class ObjectStorageClientSupport
            implements Prototype.BuilderDecorator<ObjectStorageClientConfig.BuilderBase<?, ?>> {
        private static final String REGION = "region";
        private static final String REGION_ID = "region-id";

        ObjectStorageClientSupport() {
        }

        @Override
        public void decorate(ObjectStorageClientConfig.BuilderBase<?, ?> builder) {
            builder.config().ifPresent(config -> applyRegionAlias(config, builder));
        }

        @Prototype.RuntimeTypeFactoryMethod
        static ObjectStorageClient createObjectStorageClient(ObjectStorageClientConfig config) {
            ObjectStorageClient client = config.clientConfiguration()
                    .map(clientConfig -> new ObjectStorageClient(authProvider(), clientConfig))
                    .orElseGet(() -> new ObjectStorageClient(authProvider()));
            config.endpoint().ifPresent(client::setEndpoint);
            config.region().ifPresent(client::setRegion);
            return client;
        }

        @Prototype.ConfigFactoryMethod("clientConfiguration")
        static ClientConfiguration createClientConfiguration(Config config) {
            return OciClientConfiguration.create(config).build();
        }

        private static void applyRegionAlias(io.helidon.common.config.Config config,
                                             ObjectStorageClientConfig.BuilderBase<?, ?> builder) {
            boolean canonicalExists = config.get(REGION).exists();
            boolean aliasExists = config.get(REGION_ID).exists();
            if (canonicalExists && aliasExists) {
                throw new ConfigException("Do not configure both " + REGION
                                                  + " and " + REGION_ID + "; specify only one.");
            }
            if (aliasExists) {
                builder.regionId().ifPresent(builder::region);
            }
        }
    }

    static final class ClientConfigurationSupport {

        private ClientConfigurationSupport() {
        }

        @Prototype.RuntimeTypeFactoryMethod
        static ClientConfiguration createClientConfiguration(OciClientConfiguration config) {
            var builder = ClientConfiguration.builder();
            config.circuitBreakerConfiguration().ifPresent(builder::circuitBreakerConfiguration);
            config.retryConfiguration().ifPresent(builder::retryConfiguration);
            config.connectionTimeout()
                    .ifPresent(timeout -> builder.connectionTimeoutMillis(Math.toIntExact(timeout.toMillis())));
            config.maxAsyncThreads().ifPresent(builder::maxAsyncThreads);
            config.readTimeout()
                    .ifPresent(readTimeout -> builder.readTimeoutMillis(Math.toIntExact(readTimeout.toMillis())));
            config.disableDataBufferingOnUpload().ifPresent(builder::disableDataBufferingOnUpload);
            return builder.build();
        }

        @Prototype.RuntimeTypeFactoryMethod("retryConfiguration")
        static RetryConfiguration createRetryConfiguration(OciRetryConfiguration config) {
            return RetryConfigurationSupport.createRetryConfiguration(config);
        }

        @Prototype.ConfigFactoryMethod
        static RetryConfiguration createRetryConfiguration(Config config) {
            return RetryConfigurationSupport.createRetryConfiguration(config);
        }

        @Prototype.RuntimeTypeFactoryMethod("circuitBreakerConfiguration")
        static CircuitBreakerConfiguration createCircuitBreakerConfiguration(
                OciCircuitBreakerConfiguration helidonOciCircuitBreakerConfig) {
            var ociCircuitBreakerConfigBuilder = CircuitBreakerConfiguration.builder();

            helidonOciCircuitBreakerConfig.failureRateThreshold()
                    .ifPresent(ociCircuitBreakerConfigBuilder::failureRateThreshold);
            helidonOciCircuitBreakerConfig.slowCallRateThreshold()
                    .ifPresent(ociCircuitBreakerConfigBuilder::slowCallRateThreshold);
            helidonOciCircuitBreakerConfig.waitDurationInOpenState()
                    .ifPresent(ociCircuitBreakerConfigBuilder::waitDurationInOpenState);
            helidonOciCircuitBreakerConfig.permittedNumberOfCallsInHalfOpenState()
                    .ifPresent(ociCircuitBreakerConfigBuilder::permittedNumberOfCallsInHalfOpenState);
            helidonOciCircuitBreakerConfig.minimumNumberOfCalls()
                    .ifPresent(ociCircuitBreakerConfigBuilder::minimumNumberOfCalls);
            helidonOciCircuitBreakerConfig.slidingWindowSize()
                    .ifPresent(ociCircuitBreakerConfigBuilder::slidingWindowSize);
            helidonOciCircuitBreakerConfig.slowCallDurationThreshold()
                    .ifPresent(ociCircuitBreakerConfigBuilder::slowCallDurationThreshold);
            helidonOciCircuitBreakerConfig.writableStackTraceEnabled()
                    .ifPresent(ociCircuitBreakerConfigBuilder::writableStackTraceEnabled);

            return ociCircuitBreakerConfigBuilder.build();
        }

        @Prototype.ConfigFactoryMethod
        static CircuitBreakerConfiguration createCircuitBreakerConfiguration(Config config) {
            var ociCircuitBreakerConfigBuilder = CircuitBreakerConfiguration.builder();
            config.get("failure-rate-threshold").asInt().ifPresent(ociCircuitBreakerConfigBuilder::failureRateThreshold);
            config.get("slow-call-rate-threshold").asInt().ifPresent(ociCircuitBreakerConfigBuilder::slowCallRateThreshold);
            config.get("wait-duration-in-open-state")
                    .as(java.time.Duration.class)
                    .ifPresent(ociCircuitBreakerConfigBuilder::waitDurationInOpenState);
            config.get("permitted-number-of-calls-in-half-open-state")
                    .asInt()
                    .ifPresent(ociCircuitBreakerConfigBuilder::permittedNumberOfCallsInHalfOpenState);
            config.get("minimum-number-of-calls").asInt().ifPresent(ociCircuitBreakerConfigBuilder::minimumNumberOfCalls);
            config.get("sliding-window-size").asInt().ifPresent(ociCircuitBreakerConfigBuilder::slidingWindowSize);
            config.get("slow-call-duration-threshold")
                    .as(java.time.Duration.class)
                    .ifPresent(ociCircuitBreakerConfigBuilder::slowCallDurationThreshold);
            config.get("writable-stack-trace-enabled").asBoolean()
                    .ifPresent(ociCircuitBreakerConfigBuilder::writableStackTraceEnabled);
            return ociCircuitBreakerConfigBuilder.build();
        }
    }

    static final class RetryConfigurationSupport {

        private static final Function<Config, TerminationStrategy.Type> TERMINATION_STRATEGY_TYPE_MAPPER =
                new EnumMapperProvider().mapper(TerminationStrategy.Type.class).orElseThrow();
        private static final Function<Config, DelayStrategy.Type> DELAY_STRATEGY_TYPE_MAPPER =
                new EnumMapperProvider().mapper(DelayStrategy.Type.class).orElseThrow();
        private static final Function<Config, RetryCondition.Type> RETRY_CONDITION_TYPE_MAPPER =
                new EnumMapperProvider().mapper(RetryCondition.Type.class).orElseThrow();

        private RetryConfigurationSupport() {
        }

        @Prototype.ConfigFactoryMethod("terminationStrategy")
        static com.oracle.bmc.waiter.TerminationStrategy createTerminationStrategy(Config config) {
            return switch (TERMINATION_STRATEGY_TYPE_MAPPER.apply(typeNode(config))) {
                case MAX_ATTEMPTS -> new MaxAttemptsTerminationStrategy(MaxAttemptsTerminationStrategyConfig.create(config)
                                                                                .maxAttempts());
                case MAX_TIME -> MaxTimeTerminationStrategy.ofMillis(MaxTimeTerminationStrategyConfig.create(config)
                                                                             .maxTime()
                                                                             .toMillis());
            };
        }

        @Prototype.ConfigFactoryMethod("delayStrategy")
        static com.oracle.bmc.waiter.DelayStrategy createDelayStrategy(Config config) {
            return switch (DELAY_STRATEGY_TYPE_MAPPER.apply(typeNode(config))) {
                case FIXED -> new FixedTimeDelayStrategy(FixedTimeDelayStrategyConfig.create(config)
                                                                 .delay()
                                                                 .toMillis());
                case EXPONENTIAL -> new ExponentialBackoffDelayStrategy(ExponentialBackoffDelayStrategyConfig.create(config)
                                                                                .maxDelay()
                                                                                .toMillis());
                case EXPONENTIAL_WITH_JITTER ->
                        new ExponentialBackoffDelayStrategyWithJitter(ExponentialBackoffDelayStrategyWithJitterConfig.create(
                                        config)
                                                                              .maxDelay()
                                                                              .toMillis());
            };
        }

        @Prototype.ConfigFactoryMethod("retryCondition")
        static com.oracle.bmc.retrier.RetryCondition createRetryCondition(Config config) {
            return switch (RETRY_CONDITION_TYPE_MAPPER.apply(typeNode(config))) {
                case DEFAULT -> {
                    DefaultRetryConditionConfig.create(config);
                    yield new DefaultRetryCondition();
                }
                case RETRY_ON_OPEN_CIRCUIT_BREAKER -> {
                    RetryOnOpenCircuitBreakerDefaultRetryConditionConfig.create(config);
                    yield new RetryOnOpenCircuitBreakerDefaultRetryCondition();
                }
            };
        }

        @Prototype.RuntimeTypeFactoryMethod("retryOptions")
        static RetryOptions createRetryOptions(OciRetryOptions config) {
            return config.markReadLimit()
                    .map(RetryOptions::new)
                    .orElseGet(() -> new RetryOptions(Integer.MAX_VALUE));
        }

        @Prototype.ConfigFactoryMethod("retryOptions")
        static RetryOptions createRetryOptions(Config config) {
            return createRetryOptions(OciRetryOptions.create(config));
        }

        static RetryConfiguration createRetryConfiguration(OciRetryConfiguration helidonOciRetryConfig) {
            var ociRetryConfigBuilder = RetryConfiguration.builder();
            helidonOciRetryConfig.terminationStrategy().ifPresent(ociRetryConfigBuilder::terminationStrategy);
            helidonOciRetryConfig.delayStrategy().ifPresent(ociRetryConfigBuilder::delayStrategy);
            helidonOciRetryConfig.retryCondition().ifPresent(ociRetryConfigBuilder::retryCondition);
            helidonOciRetryConfig.retryOptions().ifPresent(ociRetryConfigBuilder::retryOptions);
            return ociRetryConfigBuilder.build();
        }

        static RetryConfiguration createRetryConfiguration(Config config) {
            return createRetryConfiguration(OciRetryConfiguration.create(config));
        }

        private static Config typeNode(Config config) {
            return config.get("type");
        }
    }

    private static BasicAuthenticationDetailsProvider authProvider() {
        return Services.get(BasicAuthenticationDetailsProvider.class);
    }
}
