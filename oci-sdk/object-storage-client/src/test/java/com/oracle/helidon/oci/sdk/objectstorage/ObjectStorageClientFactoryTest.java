/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.objectstorage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.RetryOnOpenCircuitBreakerDefaultRetryCondition;
import com.oracle.bmc.waiter.ExponentialBackoffDelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.WaiterConfiguration;
import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectStorageClientFactoryTest {
    private static final String CLIENT = "oci.object-storage-client.client.";

    @Test
    void createsObjectStorageClientWithEndpointOverride() throws Exception {
        ObjectStorageClientConfig config = ObjectStorageClientConfig.builder()
                .endpoint("http://127.0.0.1:9876")
                .region("us-ashburn-1")
                .buildPrototype();

        ObjectStorageClient client = factory(config).get();

        assertThat(client, notNullValue());
        assertThat(client.getEndpoint(), is("http://127.0.0.1:9876"));
    }

    @Test
    void createsObjectStorageClientWithConfiguredRegion() throws Exception {
        ObjectStorageClientConfig config = ObjectStorageClientConfig.builder()
                .region("us-ashburn-1")
                .buildPrototype();

        ObjectStorageClient client = factory(config).get();

        assertThat(client, notNullValue());
        assertThat(client.getEndpoint(), is("https://objectstorage.us-ashburn-1.oraclecloud.com"));
    }

    @Test
    void createsObjectStorageClientWithRegionSupplierFallback() throws Exception {
        ObjectStorageClient client = new ObjectStorageClientFactory(ObjectStorageClientConfig.create(),
                                                                    authProvider(),
                                                                    () -> Region.IAD)
                .get();

        assertThat(client, notNullValue());
        assertThat(client.getEndpoint(), is("https://objectstorage.us-ashburn-1.oraclecloud.com"));
    }

    @Test
    void readsRegionIdAliasAsCanonicalRegion() {
        Config config = Config.just(ConfigSources.create(
                Map.of("oci.object-storage-client.region-id", "us-ashburn-1")));

        ObjectStorageClientConfig objectStorageConfig = new ObjectStorageClientConfigFactory(config).get();

        assertThat(objectStorageConfig.region().orElseThrow(), is("us-ashburn-1"));
    }

    @Test
    void failsWhenRegionAndRegionIdAreBothConfigured() {
        Config config = Config.just(ConfigSources.create(
                Map.of("oci.object-storage-client.region", "us-phoenix-1",
                       "oci.object-storage-client.region-id", "us-ashburn-1")));

        ConfigException exception = assertThrows(ConfigException.class,
                                                 () -> new ObjectStorageClientConfigFactory(config).get());

        assertThat(exception.getMessage(), containsString("region"));
        assertThat(exception.getMessage(), containsString("region-id"));
    }

    @Test
    void readsClientConfigurationSubtree() {
        Config config = Config.just(ConfigSources.create(
                Map.ofEntries(entry(CLIENT + "connection-timeout", "PT3S"),
                              entry(CLIENT + "read-timeout", "PT7S"),
                              entry(CLIENT + "max-async-threads", "11"),
                              entry(CLIENT + "disable-data-buffering-on-upload", "true"),
                              entry(CLIENT + "retry.termination-strategy.type", "max-attempts"),
                              entry(CLIENT + "retry.termination-strategy.max-attempts", "5"),
                              entry(CLIENT + "retry.delay-strategy.type", "exponential"),
                              entry(CLIENT + "retry.delay-strategy.max-delay", "PT2S"),
                              entry(CLIENT + "retry.retry-condition.type", "retry-on-open-circuit-breaker"),
                              entry(CLIENT + "retry.retry-options.mark-read-limit", "4096"),
                              entry(CLIENT + "circuit-breaker.failure-rate-threshold", "77"),
                              entry(CLIENT + "circuit-breaker.slow-call-rate-threshold", "66"),
                              entry(CLIENT + "circuit-breaker.wait-duration-in-open-state", "PT9S"),
                              entry(CLIENT + "circuit-breaker.permitted-number-of-calls-in-half-open-state", "4"),
                              entry(CLIENT + "circuit-breaker.minimum-number-of-calls", "3"),
                              entry(CLIENT + "circuit-breaker.sliding-window-size", "22"),
                              entry(CLIENT + "circuit-breaker.slow-call-duration-threshold", "PT8S"),
                              entry(CLIENT + "circuit-breaker.writable-stack-trace-enabled", "false"))));

        ClientConfiguration clientConfiguration = new ObjectStorageClientConfigFactory(config)
                .get()
                .client()
                .orElseThrow();

        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is((int) Duration.ofSeconds(3).toMillis()));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is((int) Duration.ofSeconds(7).toMillis()));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(11));

        RetryConfiguration retryConfiguration = clientConfiguration.getRetryConfiguration();
        assertThat(retryConfiguration, notNullValue());
        assertThat(retryConfiguration.getTerminationStrategy(), instanceOf(MaxAttemptsTerminationStrategy.class));
        MaxAttemptsTerminationStrategy terminationStrategy =
                (MaxAttemptsTerminationStrategy) retryConfiguration.getTerminationStrategy();
        assertThat(terminationStrategy.getMaxAttempts(), is(5));
        assertThat(retryConfiguration.getDelayStrategy(), instanceOf(ExponentialBackoffDelayStrategy.class));
        ExponentialBackoffDelayStrategy delayStrategy =
                (ExponentialBackoffDelayStrategy) retryConfiguration.getDelayStrategy();
        WaiterConfiguration.WaitContext waitContext = new WaiterConfiguration.WaitContext(0L);
        waitContext.incrementAttempts();
        assertThat(delayStrategy.nextDelay(waitContext), is(2000L));
        assertThat(retryConfiguration.getRetryCondition(),
                   instanceOf(RetryOnOpenCircuitBreakerDefaultRetryCondition.class));
        assertThat(retryConfiguration.getRetryOptions().getMarkReadLimit(), is(4096));

        CircuitBreakerConfiguration circuitBreakerConfiguration = clientConfiguration.getCircuitBreakerConfiguration();
        assertThat(circuitBreakerConfiguration, notNullValue());
        assertThat(circuitBreakerConfiguration.getFailureRateThreshold(), is(77));
        assertThat(circuitBreakerConfiguration.getSlowCallRateThreshold(), is(66));
        assertThat(circuitBreakerConfiguration.getWaitDurationInOpenState(), is(Duration.ofSeconds(9)));
        assertThat(circuitBreakerConfiguration.getPermittedNumberOfCallsInHalfOpenState(), is(4));
        assertThat(circuitBreakerConfiguration.getMinimumNumberOfCalls(), is(3));
        assertThat(circuitBreakerConfiguration.getSlidingWindowSize(), is(22));
        assertThat(circuitBreakerConfiguration.getSlowCallDurationThreshold(), is(Duration.ofSeconds(8)));
        assertThat(circuitBreakerConfiguration.isWritableStackTraceEnabled(), is(false));
    }

    @Test
    void generatedConfigMetadataUsesRuntimeConfigPrefix() throws Exception {
        try (InputStream stream = ObjectStorageClientFactoryTest.class.getClassLoader()
                .getResourceAsStream("META-INF/helidon/config-metadata.json")) {
            assertThat(stream, notNullValue());

            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(metadata.contains("\"prefix\":\"oci.object-storage-client\""), is(true));
            assertThat(metadata.contains("\"key\":\"region-id\""), is(true));
            assertThat(metadata.contains("retry-" + "configuration"), is(false));
            assertThat(metadata.contains("circuit-breaker-" + "configuration"), is(false));
        }
    }

    private static ObjectStorageClientFactory factory(ObjectStorageClientConfig config) throws Exception {
        return new ObjectStorageClientFactory(config, authProvider(), () -> Region.IAD);
    }

    private static BasicAuthenticationDetailsProvider authProvider() throws Exception {
        byte[] privateKey = privateKey();
        return new BasicAuthenticationDetailsProvider() {
            @Override
            public String getKeyId() {
                return "ocid1.tenancy.oc1..test/ocid1.user.oc1..test/fingerprint";
            }

            @Override
            public InputStream getPrivateKey() {
                return new ByteArrayInputStream(privateKey);
            }

            @Override
            public String getPassPhrase() {
                return null;
            }

            @Override
            public char[] getPassphraseCharacters() {
                return null;
            }
        };
    }

    private static byte[] privateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        byte[] encoded = generator.generateKeyPair().getPrivate().getEncoded();
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encoded);
        return ("-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n")
                .getBytes(StandardCharsets.US_ASCII);
    }
}
