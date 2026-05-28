/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.identity.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.identity.Identity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityClientFactoryTest {

    @Test
    void createsIdentityClientWithEndpointOverride() throws Exception {
        IdentityClientConfig config = IdentityClientConfig.builder()
                .endpoint("http://127.0.0.1:9876")
                .client(client -> client.connectionTimeout(Duration.ofSeconds(2))
                        .readTimeout(Duration.ofSeconds(5))
                        .maxAsyncThreads(8))
                .build();

        Identity identity = new IdentityClientFactory(config, authProvider()).get();

        assertThat(identity, notNullValue());
        assertThat(identity.getEndpoint(), is("http://127.0.0.1:9876"));
    }

    @Test
    void createsIdentityClientWithRegion() throws Exception {
        IdentityClientConfig config = IdentityClientConfig.builder()
                .region("us-ashburn-1")
                .build();

        Identity identity = new IdentityClientFactory(config, authProvider()).get();

        assertThat(identity, notNullValue());
        assertThat(identity.getEndpoint(), is("https://identity.us-ashburn-1.oci.oraclecloud.com"));
    }

    @Test
    void endpointOverrideTakesPrecedenceOverRegionAndRealmTemplate() throws Exception {
        IdentityClientConfig config = IdentityClientConfig.builder()
                .endpoint("http://127.0.0.1:9876")
                .region("us-ashburn-1")
                .realmSpecificEndpointTemplateEnabled(true)
                .build();

        Identity identity = new IdentityClientFactory(config, authProvider()).get();

        assertThat(identity, notNullValue());
        assertThat(identity.getEndpoint(), is("http://127.0.0.1:9876"));
    }

    @Test
    void realmSpecificTemplateUsesConfiguredRegion() throws Exception {
        IdentityClientConfig config = IdentityClientConfig.builder()
                .region("us-ashburn-1")
                .realmSpecificEndpointTemplateEnabled(true)
                .build();

        Identity identity = new IdentityClientFactory(config, authProvider()).get();

        assertThat(identity, notNullValue());
        assertThat(identity.getEndpoint(), is("https://identity.us-ashburn-1.oci.oraclecloud.com"));
    }

    @Test
    void failsWhenEndpointAndRegionAreAbsentAndAuthenticationProviderHasNoRegion() {
        IdentityClientConfig config = IdentityClientConfig.builder()
                .build();
        NullPointerException exception = assertThrows(NullPointerException.class,
                                                      () -> new IdentityClientFactory(config, authProvider()).get());

        assertThat(exception.getMessage(),
                   is("No endpoint has been configured"));
    }

    @Test
    void readsConfigFromOciIdentityClientSubtree() {
        Config config = Config.just(ConfigSources.create(
                Map.of("oci.identity-client.endpoint", "http://identity.example",
                       "oci.identity-client.client.connection-timeout", "PT3S",
                       "oci.identity-client.client.read-timeout", "PT7S",
                       "oci.identity-client.client.max-async-threads", "11")));

        IdentityClientConfig identityConfig = new IdentityClientConfigFactory(config).get();
        ClientConfiguration clientConfiguration = identityConfig.client().orElseThrow();

        assertThat(identityConfig.endpoint().orElseThrow(), is("http://identity.example"));
        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is(3000));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is(7000));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(11));
    }

    @Test
    void defaultClientConfigurationMatchesPreviousIdentityDefaults() {
        IdentityClientConfig identityConfig = IdentityClientConfig.builder()
                .build();
        ClientConfiguration clientConfiguration = identityConfig.client()
                .orElseGet(() -> ClientConfiguration.builder().build());

        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is(10000));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is(60000));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(50));
    }

    @Test
    void generatedConfigMetadataUsesRuntimeConfigPrefix() throws Exception {
        try (InputStream stream = IdentityClientFactoryTest.class.getClassLoader()
                .getResourceAsStream("META-INF/helidon/config-metadata.json")) {
            assertThat(stream, notNullValue());

            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(metadata.contains("\"prefix\":\"oci.identity-client\""), is(true));
        }
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
