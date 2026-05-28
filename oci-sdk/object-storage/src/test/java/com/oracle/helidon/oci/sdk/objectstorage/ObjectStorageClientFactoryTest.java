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
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectStorageClientFactoryTest {

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
                Map.of("oci.object-storage-client.client.connection-timeout", "PT3S",
                       "oci.object-storage-client.client.read-timeout", "PT7S",
                       "oci.object-storage-client.client.max-async-threads", "11",
                       "oci.object-storage-client.client.disable-data-buffering-on-upload", "true")));

        ClientConfiguration clientConfiguration = new ObjectStorageClientConfigFactory(config)
                .get()
                .client()
                .orElseThrow();

        assertThat(clientConfiguration.getConnectionTimeoutMillis(), is((int) Duration.ofSeconds(3).toMillis()));
        assertThat(clientConfiguration.getReadTimeoutMillis(), is((int) Duration.ofSeconds(7).toMillis()));
        assertThat(clientConfiguration.getMaxAsyncThreads(), is(11));
    }

    @Test
    void generatedConfigMetadataUsesRuntimeConfigPrefix() throws Exception {
        try (InputStream stream = ObjectStorageClientFactoryTest.class.getClassLoader()
                .getResourceAsStream("META-INF/helidon/config-metadata.json")) {
            assertThat(stream, notNullValue());

            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(metadata.contains("\"prefix\":\"oci.object-storage-client\""), is(true));
            assertThat(metadata.contains("\"key\":\"region-id\""), is(true));
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
