/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.Map;
import java.util.Optional;

import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.WaiterConfiguration;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Ssv2ClientTest {
    @Test
    void resolvesPhoenixToR2Endpoint() {
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of()), Optional.empty());

        assertThat(client.resolvedEndpoint(envConfig("r2.oracleiaas.com")),
                   is("https://secret-service-ce.r2.oracleiaas.com/v1"));
    }

    @Test
    void resolvesSeattleToR1Endpoint() {
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of()), Optional.empty());

        assertThat(client.resolvedEndpoint(envConfig("r1.oracleiaas.com")),
                   is("https://secret-service-ce.r1.oracleiaas.com/v1"));
    }

    @Test
    void resolvesRealmSpecificDomain() {
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of()), Optional.empty());

        assertThat(client.resolvedEndpoint(envConfig("uk-gov-london-1.oraclegoviaas.uk")),
                   is("https://secret-service-ce.uk-gov-london-1.oraclegoviaas.uk/v1"));
    }

    @Test
    void resolvesDefaultEndpointFromInjectedOciEnvSource() {
        ConfigSource ociEnvSource = ConfigSources.create(Map.of("oci.env.iaas-domain-name", "r1.oracleiaas.com"),
                                                         "oci-env")
                .build();
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of()), Optional.of(ociEnvSource));

        assertThat(client.resolvedEndpoint(Config.empty()),
                   is("https://secret-service-ce.r1.oracleiaas.com/v1"));
    }

    @Test
    void resolvesEnvConfigPlaceholdersInEndpointTemplate() {
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of(
                "endpoint", "https://example.${oci.env.region-internal-name}.${oci.env.realm-iaas-domain-name}/v1"
        )), Optional.empty());

        Config config = config(Map.of(
                "oci.env.region-internal-name", "r2",
                "oci.env.realm-iaas-domain-name", "oracleiaas.com"
        ));

        assertThat(client.resolvedEndpoint(config),
                   is("https://example.r2.oracleiaas.com/v1"));
    }

    @Test
    void keepsResolvedOverrideEndpoint() {
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of(
                "endpoint", "https://example.ap-chiyoda-1.oraclerealm8.com/v1"
        )), Optional.empty());

        assertThat(client.resolvedEndpoint(Config.empty()),
                   is("https://example.ap-chiyoda-1.oraclerealm8.com/v1"));
    }

    @Test
    void failsWhenDefaultEndpointPlaceholderIsUnresolved() {
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of()), Optional.empty());

        assertThrows(ConfigException.class, () -> client.resolvedEndpoint(Config.empty()));
    }

    @Test
    void mapsRetryConfiguration() {
        Ssv2Client client = new Ssv2Client(clientConfig(Map.of(
                "retry-config.max-retries", "9",
                "retry-config.min-retry-delay-in-ms", "100",
                "retry-config.max-retry-delay-in-ms", "400"
        )), Optional.empty());

        var retryConfiguration = client.clientConfiguration().getRetryConfiguration();
        var terminationStrategy = (MaxAttemptsTerminationStrategy) retryConfiguration.getTerminationStrategy();
        WaiterConfiguration.WaitContext waitContext = new WaiterConfiguration.WaitContext(0);
        waitContext.incrementAttempts();
        waitContext.incrementAttempts();
        waitContext.incrementAttempts();

        assertThat(terminationStrategy.getMaxAttempts(), is(9));
        assertThat(retryConfiguration.getDelayStrategy().nextDelay(waitContext), is(400L));
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

    private static Config envConfig(String iaasDomainName) {
        return config(Map.of("oci.env.iaas-domain-name", iaasDomainName));
    }
}
