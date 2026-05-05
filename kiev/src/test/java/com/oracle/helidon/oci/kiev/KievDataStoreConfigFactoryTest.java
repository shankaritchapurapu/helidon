/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import com.oracle.pic.kiev.DataStoreConfig;
import com.oracle.pic.kiev.DirectDbStoreConfig;
import com.oracle.pic.kiev.KaasStoreConfig;
import com.oracle.pic.kiev.auth.AuthDetailsConfig;
import com.oracle.pic.kiev.mapping.InMemoryDataStoreConfig;
import com.oracle.pic.kiev.registry.config.ClientRegistryConfig;
import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KievDataStoreConfigFactoryTest {

    @Test
    void testCreatesInMemory() {
        DataStoreConfig config = factory(Map.of(
                "oci.kiev.store-name", "store",
                "oci.kiev.app-name", "app"
        )).get();

        InMemoryDataStoreConfig dataStoreConfig = assertInstanceOf(InMemoryDataStoreConfig.class, config);
        assertEquals("store", dataStoreConfig.getStoreName());
        assertEquals("app", dataStoreConfig.getAppName());
        assertEquals(100, dataStoreConfig.getTransactionMaxReads());
        assertEquals(100, dataStoreConfig.getTransactionMaxWrites());
    }

    @Test
    void testCreatesDirectDb() {
        DataStoreConfig config = factory(Map.of(
                "oci.kiev.backend", "DIRECT_DB",
                "oci.kiev.store-name", "pdbdev",
                "oci.kiev.app-name", "KievTest",
                "oci.kiev.transaction-max-reads", "111",
                "oci.kiev.transaction-max-writes", "222",
                "oci.kiev.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev",
                "oci.kiev.direct-db.user-name", "helidon",
                "oci.kiev.direct-db.password", "changeit",
                "oci.kiev.direct-db.schema-name", "helidon"
        )).get();

        DirectDbStoreConfig dataStoreConfig = assertInstanceOf(DirectDbStoreConfig.class, config);
        assertEquals("pdbdev", dataStoreConfig.getStoreName());
        assertEquals("KievTest", dataStoreConfig.getAppName());
        assertEquals("jdbc:oracle:thin:@//localhost:1521/pdbdev", dataStoreConfig.getJDBCURL());
        assertEquals("helidon", dataStoreConfig.getUserName());
        assertEquals("changeit", dataStoreConfig.getPassword());
        assertEquals("helidon", dataStoreConfig.getSchemaName());
        assertEquals(111, dataStoreConfig.getTransactionMaxReads());
        assertEquals(222, dataStoreConfig.getTransactionMaxWrites());
    }

    @Test
    void testCreatesInstanceService() {
        DataStoreConfig config = factory(Map.ofEntries(
                Map.entry("oci.kiev.backend", "SERVICE"),
                Map.entry("oci.kiev.store-name", "remote-store"),
                Map.entry("oci.kiev.app-name", "StoreApp"),
                Map.entry("oci.kiev.transaction-max-reads", "111"),
                Map.entry("oci.kiev.transaction-max-writes", "222"),
                Map.entry("oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.service.auth.tls.cert-reload-duration", "PT5M"),
                Map.entry("oci.kiev.service.auth.tls.cert-ssl-algorithm", "SunX509")
        )).get();

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertEquals("remote-store", dataStoreConfig.getStoreName());
        assertEquals("StoreApp", dataStoreConfig.getAppName());
        assertEquals("ocid1.compartment.oc1..example", dataStoreConfig.getCompartmentId());
        assertEquals("https://frontend.example", dataStoreConfig.getFrontendEndpoint());
        assertEquals(ClientRegistryLocality.REGIONAL, dataStoreConfig.getLocality());
        assertEquals(111, dataStoreConfig.getTransactionMaxReads());
        assertEquals(222, dataStoreConfig.getTransactionMaxWrites());
        assertNull(dataStoreConfig.getRegistryConfig());

        AuthDetailsConfig.InstanceAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.InstanceAuthDetailsConfig.class, dataStoreConfig.getAuthDetailsConfig());
        assertEquals("https://auth.example", authConfig.getAuthEndpoint());
        assertEquals("/tmp/root.pem", authConfig.getRootCertPemPath());
        assertEquals(Duration.ofMinutes(5), authConfig.getCertReloadDuration());
        assertEquals("SunX509", authConfig.getCertSslAlgorithm());
    }

    @Test
    void testCreatesS2sService() {
        DataStoreConfig config = factory(Map.ofEntries(
                Map.entry("oci.kiev.backend", "SERVICE"),
                Map.entry("oci.kiev.store-name", "remote-store"),
                Map.entry("oci.kiev.app-name", "StoreApp"),
                Map.entry("oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.service.locality", "AD1"),
                Map.entry("oci.kiev.service.auth.type", "S2S"),
                Map.entry("oci.kiev.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.service.auth.s2s.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.service.auth.s2s.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.service.auth.s2s.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.service.auth.s2s.tenant-id", "ocid1.tenancy.oc1..example"),
                Map.entry("oci.kiev.service.auth.s2s.key-passphrase", "secret"),
                Map.entry("oci.kiev.service.auth.tls.cert-reload-duration", "PT15M"),
                Map.entry("oci.kiev.service.auth.tls.cert-ssl-algorithm", "SunX509")
        )).get();

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertEquals(ClientRegistryLocality.AD1, dataStoreConfig.getLocality());

        AuthDetailsConfig.S2sAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.S2sAuthDetailsConfig.class, dataStoreConfig.getAuthDetailsConfig());
        assertEquals("https://auth.example", authConfig.getAuthEndpoint());
        assertEquals("/tmp/root.pem", authConfig.getRootCertPemPath());
        assertEquals("/tmp/leaf.pem", authConfig.getLeafCertPath());
        assertEquals("/tmp/leaf.key", authConfig.getLeafCertKeyPath());
        assertEquals("/tmp/intermediate.pem", authConfig.getIntermediateCertPath());
        assertEquals("ocid1.tenancy.oc1..example", authConfig.getTenantId());
        assertEquals("secret", authConfig.getKeyPassphrase());
        assertEquals(Duration.ofMinutes(15), authConfig.getCertReloadDuration());
        assertEquals("SunX509", authConfig.getCertSslAlgorithm());
    }

    @Test
    void testCreatesKiabLocalService() {
        DataStoreConfig config = factory(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "kaaspdb",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.service.auth.type", "KIAB_LOCAL"
        )).get();

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        AuthDetailsConfig.OverriddenAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.OverriddenAuthDetailsConfig.class, dataStoreConfig.getAuthDetailsConfig());
        assertNotNull(authConfig.getAuthProviderOverride());

        ClientRegistryConfig registryConfig = dataStoreConfig.getRegistryConfig();
        assertNotNull(registryConfig);
        assertEquals(Boolean.FALSE, registryConfig.getEnabled());
        assertEquals("http://localhost:16666", registryConfig.getEndpointOverride());
    }

    @Test
    void testCreatesOverriddenService() {
        BasicAuthenticationDetailsProvider authProvider = new TestBasicAuthenticationDetailsProvider();

        DataStoreConfig config = factory(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "remote-store",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.service.auth.type", "OVERRIDDEN"
        ), () -> Optional.of(authProvider)).get();

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        AuthDetailsConfig.OverriddenAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.OverriddenAuthDetailsConfig.class, dataStoreConfig.getAuthDetailsConfig());
        assertSame(authProvider, authConfig.getAuthProviderOverride());
        assertNull(dataStoreConfig.getRegistryConfig());
    }

    @Test
    void testCreatesOverriddenServiceWithTls() {
        BasicAuthenticationDetailsProvider authProvider = new TestBasicAuthenticationDetailsProvider();

        DataStoreConfig config = factory(Map.ofEntries(
                Map.entry("oci.kiev.backend", "SERVICE"),
                Map.entry("oci.kiev.store-name", "remote-store"),
                Map.entry("oci.kiev.app-name", "StoreApp"),
                Map.entry("oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.service.auth.type", "OVERRIDDEN"),
                Map.entry("oci.kiev.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.service.auth.tls.cert-reload-duration", "PT5M"),
                Map.entry("oci.kiev.service.auth.tls.cert-ssl-algorithm", "SunX509")
        ), () -> Optional.of(authProvider)).get();

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        AuthDetailsConfig.OverriddenAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.OverriddenAuthDetailsConfig.class, dataStoreConfig.getAuthDetailsConfig());
        assertSame(authProvider, authConfig.getAuthProviderOverride());
        DynamicSslContextProviderConfig dynamicSslConfig = authConfig.getDynamicSslContextProviderConfig();
        assertNotNull(dynamicSslConfig);
        assertEquals("/tmp/root.pem", dynamicSslConfig.getRootCertPath());
        assertNull(dynamicSslConfig.getLeafCertPath());
        assertNull(dynamicSslConfig.getLeafCertKeyPath());
        assertEquals(Duration.ofMinutes(5), dynamicSslConfig.getDuration());
        assertEquals("SunX509", dynamicSslConfig.getSslAlgorithm());
    }

    @Test
    void testDoesNotResolveAuthProviderForNonOverriddenBackends() {
        Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider = () -> {
            throw new AssertionError("Auth provider should only be resolved for OVERRIDDEN auth");
        };

        factory(Map.of(
                "oci.kiev.store-name", "store",
                "oci.kiev.app-name", "app"
        ), authProvider).get();

        factory(Map.of(
                "oci.kiev.backend", "DIRECT_DB",
                "oci.kiev.store-name", "pdbdev",
                "oci.kiev.app-name", "KievTest",
                "oci.kiev.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev",
                "oci.kiev.direct-db.user-name", "helidon",
                "oci.kiev.direct-db.password", "changeit"
        ), authProvider).get();

        factory(Map.ofEntries(
                Map.entry("oci.kiev.backend", "SERVICE"),
                Map.entry("oci.kiev.store-name", "remote-store"),
                Map.entry("oci.kiev.app-name", "StoreApp"),
                Map.entry("oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.service.auth.tls.root-cert-pem-path", "/tmp/root.pem")
        ), authProvider).get();

        factory(Map.ofEntries(
                Map.entry("oci.kiev.backend", "SERVICE"),
                Map.entry("oci.kiev.store-name", "remote-store"),
                Map.entry("oci.kiev.app-name", "StoreApp"),
                Map.entry("oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.service.auth.type", "S2S"),
                Map.entry("oci.kiev.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.service.auth.s2s.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.service.auth.s2s.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.service.auth.s2s.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.service.auth.s2s.tenant-id", "ocid1.tenancy.oc1..example")
        ), authProvider).get();
    }

    @Test
    void testFailsWithoutDirectDb() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory(Map.of(
                "oci.kiev.backend", "DIRECT_DB",
                "oci.kiev.store-name", "pdbdev",
                "oci.kiev.app-name", "KievTest"
        )).get());

        assertEquals("oci.kiev.direct-db must be configured for DIRECT_DB backend", ex.getMessage());
    }

    @Test
    void testFailsWithoutService() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "store",
                "oci.kiev.app-name", "app"
        )).get());

        assertEquals("oci.kiev.service must be configured for SERVICE backend", ex.getMessage());
    }

    @Test
    void testFailsWithoutS2sAuthEndpoint() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory(Map.ofEntries(
                Map.entry("oci.kiev.backend", "SERVICE"),
                Map.entry("oci.kiev.store-name", "remote-store"),
                Map.entry("oci.kiev.app-name", "StoreApp"),
                Map.entry("oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.service.auth.type", "S2S"),
                Map.entry("oci.kiev.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.service.auth.s2s.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.service.auth.s2s.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.service.auth.s2s.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.service.auth.s2s.tenant-id", "ocid1.tenancy.oc1..example")
        )).get());

        assertEquals("oci.kiev.service.auth.auth-endpoint must be configured", ex.getMessage());
    }

    @Test
    void testFailsWithoutS2sSection() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "remote-store",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.service.auth.type", "S2S",
                "oci.kiev.service.auth.auth-endpoint", "https://auth.example",
                "oci.kiev.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"
        )).get());

        assertEquals("oci.kiev.service.auth.s2s must be configured", ex.getMessage());
    }

    @Test
    void testFailsWithoutInstanceTls() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "remote-store",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.service.auth.type", "INSTANCE"
        )).get());

        assertEquals("oci.kiev.service.auth.tls must be configured", ex.getMessage());
    }

    @Test
    void testFailsWithoutOverriddenAuthProvider() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "remote-store",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.service.auth.type", "OVERRIDDEN"
        )).get());

        assertEquals("oci.kiev.service.auth.type=OVERRIDDEN requires BasicAuthenticationDetailsProvider to be available",
                     ex.getMessage());
    }

    private static KievDataStoreConfigFactory factory(Map<String, String> values) {
        return factory(values, () -> Optional.empty());
    }

    private static KievDataStoreConfigFactory factory(Map<String, String> values,
                                                      Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider) {
        Config config = Config.just(ConfigSources.create(values));
        KievConfig kievConfig = new KievConfigFactory(config).get();
        return new KievDataStoreConfigFactory(kievConfig, authProvider);
    }

    private static final class TestBasicAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {
        @Override
        public String getKeyId() {
            return "test-key-id";
        }

        @Override
        public InputStream getPrivateKey() {
            return InputStream.nullInputStream();
        }

        @Override
        public String getPassPhrase() {
            return null;
        }

        @Override
        public char[] getPassphraseCharacters() {
            return null;
        }
    }
}
