/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.WaiterConfiguration;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Ssv2ClientTest {
    @Test
    void resolvesPhoenixToR2Endpoint() {
        Ssv2ClientConfig config = resolvedClientConfig(Map.of(),
                                                       () -> Optional.of(ociEnvSource("r2.oracleiaas.com")));

        assertThat(config.endpoint(),
                   is("https://secret-service-ce.r2.oracleiaas.com/v1"));
    }

    @Test
    void resolvesSeattleToR1Endpoint() {
        Ssv2ClientConfig config = resolvedClientConfig(Map.of(),
                                                       () -> Optional.of(ociEnvSource("r1.oracleiaas.com")));

        assertThat(config.endpoint(),
                   is("https://secret-service-ce.r1.oracleiaas.com/v1"));
    }

    @Test
    void resolvesRealmSpecificDomain() {
        Ssv2ClientConfig config = resolvedClientConfig(
                Map.of(),
                () -> Optional.of(ociEnvSource("uk-gov-london-1.oraclegoviaas.uk")));

        assertThat(config.endpoint(),
                   is("https://secret-service-ce.uk-gov-london-1.oraclegoviaas.uk/v1"));
    }

    @Test
    void resolvesDefaultEndpointFromInjectedOciEnvSource() {
        ConfigSource ociEnvSource = ociEnvSource("r1.oracleiaas.com");
        Ssv2ClientConfig config = resolvedClientConfig(Map.of(), () -> Optional.of(ociEnvSource));

        assertThat(config.endpoint(),
                   is("https://secret-service-ce.r1.oracleiaas.com/v1"));
    }

    @Test
    void resolvesDefaultEndpointFromLazyOciEnvSource() {
        AtomicReference<Optional<ConfigSource>> source = new AtomicReference<>(Optional.empty());

        source.set(Optional.of(ConfigSources.create(Map.of("oci.env.iaas-domain-name", "r2.oracleiaas.com"),
                                                    "oci-env")
                                       .build()));

        assertThat(resolvedClientConfig(Map.of(), source::get).endpoint(),
                   is("https://secret-service-ce.r2.oracleiaas.com/v1"));
    }

    @Test
    void resolvesEnvConfigPlaceholdersInEndpointTemplate() {
        ConfigSource ociEnvSource = ociEnvSource(Map.of(
                "oci.env.region-internal-name", "r2",
                "oci.env.realm-iaas-domain-name", "oracleiaas.com"
        ));
        Ssv2ClientConfig config = resolvedClientConfig(
                Map.of("endpoint", "https://example.${oci.env.region-internal-name}.${oci.env.realm-iaas-domain-name}/v1"),
                () -> Optional.of(ociEnvSource));

        assertThat(config.endpoint(),
                   is("https://example.r2.oracleiaas.com/v1"));
    }

    @Test
    void keepsResolvedOverrideEndpoint() {
        Ssv2ClientConfig config = resolvedClientConfig(Map.of(
                "endpoint", "https://example.ap-chiyoda-1.oraclerealm8.com/v1"
        ), Optional::empty);

        assertThat(config.endpoint(),
                   is("https://example.ap-chiyoda-1.oraclerealm8.com/v1"));
    }

    @Test
    void failsWhenDefaultEndpointPlaceholderIsUnresolved() {
        ConfigException exception = assertThrows(ConfigException.class,
                                                 () -> resolvedClientConfig(Map.of(), Optional::empty));

        assertThat(exception.getMessage(), containsString("endpoint"));
    }

    @Test
    void disabledClientDoesNotResolveEndpointPlaceholders() {
        Ssv2ClientConfig config = resolvedClientConfig(Map.of(
                "enabled", "false"
        ), Optional::empty);

        assertThat(config.enabled(), is(false));
        assertThat(config.endpoint(), is(DefaultSsv2Client.DEFAULT_ENDPOINT));
    }

    @Test
    void mapsRetryConfiguration() {
        DefaultSsv2Client client = new DefaultSsv2Client(clientConfig(Map.of(
                "retry-config.max-retries", "9",
                "retry-config.min-retry-delay-in-ms", "100",
                "retry-config.max-retry-delay-in-ms", "400"
        )));

        var retryConfiguration = client.clientConfiguration().getRetryConfiguration();
        var terminationStrategy = (MaxAttemptsTerminationStrategy) retryConfiguration.getTerminationStrategy();
        WaiterConfiguration.WaitContext waitContext = new WaiterConfiguration.WaitContext(0);
        waitContext.incrementAttempts();
        waitContext.incrementAttempts();
        waitContext.incrementAttempts();

        assertThat(terminationStrategy.getMaxAttempts(), is(9));
        assertThat(retryConfiguration.getDelayStrategy().nextDelay(waitContext), is(400L));
    }

    @Test
    void mapsRetryAlias() {
        DefaultSsv2Client client = new DefaultSsv2Client(clientConfig(Map.of(
                "retry.max-retries", "7",
                "retry.min-retry-delay-in-ms", "100",
                "retry.max-retry-delay-in-ms", "300"
        )));

        var retryConfiguration = client.clientConfiguration().getRetryConfiguration();
        var terminationStrategy = (MaxAttemptsTerminationStrategy) retryConfiguration.getTerminationStrategy();
        WaiterConfiguration.WaitContext waitContext = new WaiterConfiguration.WaitContext(0);
        waitContext.incrementAttempts();
        waitContext.incrementAttempts();
        waitContext.incrementAttempts();

        assertThat(terminationStrategy.getMaxAttempts(), is(7));
        assertThat(retryConfiguration.getDelayStrategy().nextDelay(waitContext), is(300L));
    }

    @Test
    void failsWhenRetryConfigAndRetryAreConfigured() {
        assertThrows(IllegalArgumentException.class, () -> clientConfig(Map.of(
                "retry-config.max-retries", "9",
                "retry.max-retries", "7"
        )));
    }

    private static Config config(Map<String, String> values) {
        return Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(values))
                .build();
    }

    private static Ssv2ClientConfig clientConfig(Map<String, String> values) {
        return Ssv2ClientConfig.create(config(values));
    }

    private static Ssv2ClientConfig resolvedClientConfig(Map<String, String> values,
                                                         Supplier<Optional<ConfigSource>> ociEnvConfigSource) {
        return SecretServiceConfigSource.builder()
                .clientConfig(config(values))
                .ociEnvConfigSource(ociEnvConfigSource)
                .resolvedClientConfig();
    }

    private static ConfigSource ociEnvSource(String iaasDomainName) {
        return ociEnvSource(Map.of("oci.env.iaas-domain-name", iaasDomainName));
    }

    private static ConfigSource ociEnvSource(Map<String, String> values) {
        return ConfigSources.create(values, "oci-env")
                .build();
    }
}
