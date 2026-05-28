/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common;

import java.util.function.Function;

import io.helidon.builder.api.Prototype;
import io.helidon.config.Config;
import io.helidon.config.EnumMapperProvider;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
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
 * Runtime config helpers for common OCI SDK configuration blueprints.
 */
public final class ConfigSupport {

    private ConfigSupport() {
    }

    /**
     * Custom methods for OCI SDK client configuration.
     */
    public static final class ClientConfigurationSupport {

        private ClientConfigurationSupport() {
        }

        /**
         * Creates an OCI SDK client configuration from Helidon config.
         *
         * @param config Helidon config node
         * @return OCI SDK client configuration
         */
        @Prototype.ConfigFactoryMethod
        public static ClientConfiguration createClientConfiguration(Config config) {
            return OciClientConfiguration.create(config).build();
        }

        /**
         * Creates an OCI SDK client configuration from the generated prototype.
         *
         * @param config generated client configuration
         * @return OCI SDK client configuration
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static ClientConfiguration createClientConfiguration(OciClientConfiguration config) {
            var builder = ClientConfiguration.builder();
            config.circuitBreaker().ifPresent(builder::circuitBreakerConfiguration);
            config.retry().ifPresent(builder::retryConfiguration);
            config.connectionTimeout()
                    .ifPresent(timeout -> builder.connectionTimeoutMillis(Math.toIntExact(timeout.toMillis())));
            config.maxAsyncThreads().ifPresent(builder::maxAsyncThreads);
            config.readTimeout()
                    .ifPresent(readTimeout -> builder.readTimeoutMillis(Math.toIntExact(readTimeout.toMillis())));
            config.disableDataBufferingOnUpload().ifPresent(builder::disableDataBufferingOnUpload);
            return builder.build();
        }

        /**
         * Creates nested retry for client configuration.
         *
         * @param config Helidon config node
         * @return OCI SDK retry configuration
         */
        @Prototype.ConfigFactoryMethod("retry")
        public static RetryConfiguration createRetryConfiguration(Config config) {
            return RetryConfigurationSupport.createRetryConfiguration(config);
        }

        /**
         * Creates nested circuit breaker for client configuration.
         *
         * @param config Helidon config node
         * @return OCI SDK circuit breaker configuration
         */
        @Prototype.ConfigFactoryMethod("circuitBreaker")
        public static CircuitBreakerConfiguration createCircuitBreakerConfiguration(Config config) {
            return CircuitBreakerConfigurationSupport.createCircuitBreakerConfiguration(config);
        }
    }

    /**
     * Custom methods for options that expose OCI SDK client configuration directly.
     */
    public static final class ClientConfigurationOptionSupport {

        private ClientConfigurationOptionSupport() {
        }

        /**
         * Creates an OCI SDK client configuration from the generated prototype.
         *
         * @param config generated client configuration
         * @return OCI SDK client configuration
         */
        @Prototype.RuntimeTypeFactoryMethod("client")
        public static ClientConfiguration createClientConfiguration(OciClientConfiguration config) {
            return ClientConfigurationSupport.createClientConfiguration(config);
        }

        /**
         * Uses an existing OCI SDK client configuration as-is.
         *
         * @param config OCI SDK client configuration
         * @return OCI SDK client configuration
         */
        public static ClientConfiguration createClientConfiguration(ClientConfiguration config) {
            return config;
        }
    }

    /**
     * Custom methods for OCI SDK retry configuration.
     */
    public static final class RetryConfigurationSupport {

        private RetryConfigurationSupport() {
        }

        /**
         * Creates an OCI SDK retry configuration from Helidon config.
         *
         * @param config Helidon config node
         * @return OCI SDK retry configuration
         */
        @Prototype.ConfigFactoryMethod
        public static RetryConfiguration createRetryConfiguration(Config config) {
            return OciRetryConfiguration.create(config).build();
        }

        /**
         * Creates an OCI SDK retry configuration from the generated prototype.
         *
         * @param config generated retry configuration
         * @return OCI SDK retry configuration
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static RetryConfiguration createRetryConfiguration(OciRetryConfiguration config) {
            var builder = RetryConfiguration.builder();
            config.terminationStrategy().ifPresent(builder::terminationStrategy);
            config.delayStrategy().ifPresent(builder::delayStrategy);
            config.retryCondition().ifPresent(builder::retryCondition);
            config.retryOptions().ifPresent(builder::retryOptions);
            return builder.build();
        }

        /**
         * Creates nested termination strategy for retry configuration.
         *
         * @param config Helidon config node
         * @return OCI SDK termination strategy
         */
        @Prototype.ConfigFactoryMethod("terminationStrategy")
        public static com.oracle.bmc.waiter.TerminationStrategy createTerminationStrategy(Config config) {
            return TerminationStrategySupport.createTerminationStrategy(config);
        }

        /**
         * Creates nested delay strategy for retry configuration.
         *
         * @param config Helidon config node
         * @return OCI SDK delay strategy
         */
        @Prototype.ConfigFactoryMethod("delayStrategy")
        public static com.oracle.bmc.waiter.DelayStrategy createDelayStrategy(Config config) {
            return DelayStrategySupport.createDelayStrategy(config);
        }

        /**
         * Creates nested retry condition for retry configuration.
         *
         * @param config Helidon config node
         * @return OCI SDK retry condition
         */
        @Prototype.ConfigFactoryMethod("retryCondition")
        public static com.oracle.bmc.retrier.RetryCondition createRetryCondition(Config config) {
            return RetryConditionSupport.createRetryCondition(config);
        }

        /**
         * Creates nested retry options for retry configuration.
         *
         * @param config Helidon config node
         * @return OCI SDK retry options
         */
        @Prototype.ConfigFactoryMethod("retryOptions")
        public static RetryOptions createRetryOptions(Config config) {
            return RetryOptionsSupport.createRetryOptions(config);
        }
    }

    /**
     * Custom methods for OCI SDK circuit breaker configuration.
     */
    public static final class CircuitBreakerConfigurationSupport {

        private CircuitBreakerConfigurationSupport() {
        }

        /**
         * Creates an OCI SDK circuit breaker configuration from Helidon config.
         *
         * @param config Helidon config node
         * @return OCI SDK circuit breaker configuration
         */
        @Prototype.ConfigFactoryMethod
        public static CircuitBreakerConfiguration createCircuitBreakerConfiguration(Config config) {
            return OciCircuitBreakerConfiguration.create(config).build();
        }

        /**
         * Creates an OCI SDK circuit breaker configuration from the generated prototype.
         *
         * @param config generated circuit breaker configuration
         * @return OCI SDK circuit breaker configuration
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static CircuitBreakerConfiguration createCircuitBreakerConfiguration(OciCircuitBreakerConfiguration config) {
            var builder = CircuitBreakerConfiguration.builder();
            config.failureRateThreshold().ifPresent(builder::failureRateThreshold);
            config.slowCallRateThreshold().ifPresent(builder::slowCallRateThreshold);
            config.waitDurationInOpenState().ifPresent(builder::waitDurationInOpenState);
            config.permittedNumberOfCallsInHalfOpenState().ifPresent(builder::permittedNumberOfCallsInHalfOpenState);
            config.minimumNumberOfCalls().ifPresent(builder::minimumNumberOfCalls);
            config.slidingWindowSize().ifPresent(builder::slidingWindowSize);
            config.slowCallDurationThreshold().ifPresent(builder::slowCallDurationThreshold);
            config.writableStackTraceEnabled().ifPresent(builder::writableStackTraceEnabled);
            return builder.build();
        }
    }

    /**
     * Custom methods for OCI SDK retry options.
     */
    public static final class RetryOptionsSupport {

        private RetryOptionsSupport() {
        }

        /**
         * Creates OCI SDK retry options from Helidon config.
         *
         * @param config Helidon config node
         * @return OCI SDK retry options
         */
        @Prototype.ConfigFactoryMethod
        public static RetryOptions createRetryOptions(Config config) {
            return OciRetryOptions.create(config).build();
        }

        /**
         * Creates OCI SDK retry options from the generated prototype.
         *
         * @param config generated retry options
         * @return OCI SDK retry options
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static RetryOptions createRetryOptions(OciRetryOptions config) {
            return config.markReadLimit()
                    .map(RetryOptions::new)
                    .orElseGet(() -> new RetryOptions(Integer.MAX_VALUE));
        }
    }

    /**
     * Custom methods for OCI SDK retry termination strategies.
     */
    public static final class TerminationStrategySupport {

        private static final Function<Config, TerminationStrategy.Type> TYPE_MAPPER =
                new EnumMapperProvider().mapper(TerminationStrategy.Type.class).orElseThrow();

        private TerminationStrategySupport() {
        }

        /**
         * Creates an OCI SDK termination strategy from Helidon config.
         *
         * @param config Helidon config node
         * @return OCI SDK termination strategy
         */
        @Prototype.ConfigFactoryMethod
        public static com.oracle.bmc.waiter.TerminationStrategy createTerminationStrategy(Config config) {
            return switch (TYPE_MAPPER.apply(typeNode(config))) {
                case MAX_ATTEMPTS -> MaxAttemptsTerminationStrategyConfig.create(config).build();
                case MAX_TIME -> MaxTimeTerminationStrategyConfig.create(config).build();
            };
        }

        /**
         * Creates an OCI SDK max-attempts termination strategy from the generated prototype.
         *
         * @param config generated max-attempts strategy configuration
         * @return OCI SDK max-attempts termination strategy
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static com.oracle.bmc.waiter.TerminationStrategy createTerminationStrategy(
                MaxAttemptsTerminationStrategyConfig config) {
            return new MaxAttemptsTerminationStrategy(config.maxAttempts());
        }

        /**
         * Creates an OCI SDK max-time termination strategy from the generated prototype.
         *
         * @param config generated max-time strategy configuration
         * @return OCI SDK max-time termination strategy
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static com.oracle.bmc.waiter.TerminationStrategy createTerminationStrategy(
                MaxTimeTerminationStrategyConfig config) {
            return MaxTimeTerminationStrategy.ofMillis(config.maxTime().toMillis());
        }
    }

    /**
     * Custom methods for OCI SDK retry delay strategies.
     */
    public static final class DelayStrategySupport {

        private static final Function<Config, DelayStrategy.Type> TYPE_MAPPER =
                new EnumMapperProvider().mapper(DelayStrategy.Type.class).orElseThrow();

        private DelayStrategySupport() {
        }

        /**
         * Creates an OCI SDK delay strategy from Helidon config.
         *
         * @param config Helidon config node
         * @return OCI SDK delay strategy
         */
        @Prototype.ConfigFactoryMethod
        public static com.oracle.bmc.waiter.DelayStrategy createDelayStrategy(Config config) {
            return switch (TYPE_MAPPER.apply(typeNode(config))) {
                case FIXED -> FixedTimeDelayStrategyConfig.create(config).build();
                case EXPONENTIAL -> ExponentialBackoffDelayStrategyConfig.create(config).build();
                case EXPONENTIAL_WITH_JITTER -> ExponentialBackoffDelayStrategyWithJitterConfig.create(config).build();
            };
        }

        /**
         * Creates an OCI SDK fixed-time delay strategy from the generated prototype.
         *
         * @param config generated fixed-time delay strategy configuration
         * @return OCI SDK fixed-time delay strategy
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static com.oracle.bmc.waiter.DelayStrategy createDelayStrategy(FixedTimeDelayStrategyConfig config) {
            return new FixedTimeDelayStrategy(config.delay().toMillis());
        }

        /**
         * Creates an OCI SDK exponential-backoff delay strategy from the generated prototype.
         *
         * @param config generated exponential-backoff delay strategy configuration
         * @return OCI SDK exponential-backoff delay strategy
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static com.oracle.bmc.waiter.DelayStrategy createDelayStrategy(
                ExponentialBackoffDelayStrategyConfig config) {
            return new ExponentialBackoffDelayStrategy(config.maxDelay().toMillis());
        }

        /**
         * Creates an OCI SDK exponential-backoff-with-jitter delay strategy from the generated prototype.
         *
         * @param config generated exponential-backoff-with-jitter delay strategy configuration
         * @return OCI SDK exponential-backoff-with-jitter delay strategy
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static com.oracle.bmc.waiter.DelayStrategy createDelayStrategy(
                ExponentialBackoffDelayStrategyWithJitterConfig config) {
            return new ExponentialBackoffDelayStrategyWithJitter(config.maxDelay().toMillis());
        }
    }

    /**
     * Custom methods for OCI SDK retry conditions.
     */
    public static final class RetryConditionSupport {

        private static final Function<Config, RetryCondition.Type> TYPE_MAPPER =
                new EnumMapperProvider().mapper(RetryCondition.Type.class).orElseThrow();

        private RetryConditionSupport() {
        }

        /**
         * Creates an OCI SDK retry condition from Helidon config.
         *
         * @param config Helidon config node
         * @return OCI SDK retry condition
         */
        @Prototype.ConfigFactoryMethod
        public static com.oracle.bmc.retrier.RetryCondition createRetryCondition(Config config) {
            return switch (TYPE_MAPPER.apply(typeNode(config))) {
                case DEFAULT -> DefaultRetryConditionConfig.create(config).build();
                case RETRY_ON_OPEN_CIRCUIT_BREAKER ->
                        RetryOnOpenCircuitBreakerDefaultRetryConditionConfig.create(config).build();
            };
        }

        /**
         * Creates an OCI SDK default retry condition from the generated prototype.
         *
         * @param config generated default retry condition configuration
         * @return OCI SDK default retry condition
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static com.oracle.bmc.retrier.RetryCondition createRetryCondition(DefaultRetryConditionConfig config) {
            return new DefaultRetryCondition();
        }

        /**
         * Creates an OCI SDK open-circuit retry condition from the generated prototype.
         *
         * @param config generated open-circuit retry condition configuration
         * @return OCI SDK open-circuit retry condition
         */
        @Prototype.RuntimeTypeFactoryMethod
        public static com.oracle.bmc.retrier.RetryCondition createRetryCondition(
                RetryOnOpenCircuitBreakerDefaultRetryConditionConfig config) {
            return new RetryOnOpenCircuitBreakerDefaultRetryCondition();
        }
    }

    private static Config typeNode(Config config) {
        return config.get("type");
    }
}
