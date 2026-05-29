/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.Lookup;
import io.helidon.service.registry.Qualifier;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.CompositeClientConfigurator;
import com.oracle.pic.commons.s2s.config.SslTrustStoreConfigurator;
import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.DataStoreConfig;
import com.oracle.pic.kiev.DirectDbStoreConfig;
import com.oracle.pic.kiev.KaasStoreConfig;
import com.oracle.pic.kiev.auth.AuthDetailsConfig;
import com.oracle.pic.kiev.mapping.InMemoryDataStoreConfig;
import com.oracle.pic.kiev.mapping.MappedDataStore;
import com.oracle.pic.kiev.registry.config.ClientRegistryConfig;
import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KievDataStoreConfigFactoryTest {
    @Test
    void testCreatesInMemory() {
        DataStoreConfig config = dataStoreConfig(Map.of(
                "oci.kiev.data-stores.0.store-name", "store",
                "oci.kiev.data-stores.0.app-name", "app"
        ), "store");

        InMemoryDataStoreConfig dataStoreConfig = assertInstanceOf(InMemoryDataStoreConfig.class, config);
        assertEquals("store", dataStoreConfig.getStoreName());
        assertEquals("app", dataStoreConfig.getAppName());
        assertEquals(100, dataStoreConfig.getTransactionMaxReads());
        assertEquals(100, dataStoreConfig.getTransactionMaxWrites());
    }

    @Test
    void testCreatesDirectDb() {
        DataStoreConfig config = dataStoreConfig(Map.of(
                "oci.kiev.data-stores.0.backend", "DIRECT_DB",
                "oci.kiev.data-stores.0.store-name", "pdbdev",
                "oci.kiev.data-stores.0.app-name", "KievTest",
                "oci.kiev.data-stores.0.transaction-max-reads", "111",
                "oci.kiev.data-stores.0.transaction-max-writes", "222",
                "oci.kiev.data-stores.0.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev",
                "oci.kiev.data-stores.0.direct-db.user-name", "helidon",
                "oci.kiev.data-stores.0.direct-db.password", "changeit",
                "oci.kiev.data-stores.0.direct-db.schema-name", "helidon"
        ), "pdbdev");

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
        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.transaction-max-reads", "111"),
                Map.entry("oci.kiev.data-stores.0.transaction-max-writes", "222"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.cert-reload-duration", "PT5M"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.cert-ssl-algorithm", "SunX509")
        ), "remote-store");

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
                assertInstanceOf(AuthDetailsConfig.InstanceAuthDetailsConfig.class,
                                 dataStoreConfig.getAuthDetailsConfig());
        assertEquals("https://auth.example", authConfig.getAuthEndpoint());
        assertEquals("/tmp/root.pem", authConfig.getRootCertPemPath());
        assertEquals(Duration.ofMinutes(5), authConfig.getCertReloadDuration());
        assertEquals("SunX509", authConfig.getCertSslAlgorithm());
    }

    @Test
    void testDefaultsServiceLocalityFromOciEnv() {
        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.env.ad-number", "ad2"),
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem")
        ), "remote-store");

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertEquals(ClientRegistryLocality.AD2, dataStoreConfig.getLocality());
    }

    @Test
    void testFallsBackToGeneratedLocalityWhenOciEnvAdNumberIsUnknown() {
        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.env.ad-number", "unknown"),
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem")
        ), "remote-store");

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertEquals(ClientRegistryLocality.REGIONAL, dataStoreConfig.getLocality());
    }

    @Test
    void testExplicitServiceLocalityWinsOverOciEnvDefault() {
        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.env.ad-number", "ad2"),
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.locality", "REGIONAL"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem")
        ), "remote-store");

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertEquals(ClientRegistryLocality.REGIONAL, dataStoreConfig.getLocality());
    }

    @Test
    void testCreatesS2sService() {
        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.locality", "AD1"),
                Map.entry("oci.kiev.data-stores.0.service.auth.type", "S2S"),
                Map.entry("oci.kiev.data-stores.0.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.tenant-id", "ocid1.tenancy.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.key-passphrase", "secret"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.cert-reload-duration", "PT15M"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.cert-ssl-algorithm", "SunX509")
        ), "remote-store");

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertEquals(ClientRegistryLocality.AD1, dataStoreConfig.getLocality());

        AuthDetailsConfig.S2sAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.S2sAuthDetailsConfig.class,
                                 dataStoreConfig.getAuthDetailsConfig());
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
    void testInstanceAuthUsesFactorySslConfigurator(@TempDir Path tempDir) throws IOException {
        String rootCertPemPath = Files.createFile(tempDir.resolve("root.pem")).toString();
        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", rootCertPemPath)
        ), "remote-store");

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertUsesFactorySslConfigurator(dataStoreConfig.getAuthDetailsConfig().buildClientConfigurator());
    }

    @Test
    void testS2sAuthUsesFactorySslConfigurator(@TempDir Path tempDir) throws IOException {
        String rootCertPemPath = Files.createFile(tempDir.resolve("root.pem")).toString();
        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.type", "S2S"),
                Map.entry("oci.kiev.data-stores.0.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", rootCertPemPath),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.tenant-id", "ocid1.tenancy.oc1..example")
        ), "remote-store");

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        assertUsesFactorySslConfigurator(dataStoreConfig.getAuthDetailsConfig().buildClientConfigurator());
    }

    @Test
    void testCreatesKiabLocalService() {
        DataStoreConfig config = dataStoreConfig(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.data-stores.0.service.auth.type", "KIAB_LOCAL"
        ), "kaaspdb");

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        AuthDetailsConfig.OverriddenAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.OverriddenAuthDetailsConfig.class,
                                 dataStoreConfig.getAuthDetailsConfig());
        assertNotNull(authConfig.getAuthProviderOverride());

        assertRegistryDisabled(dataStoreConfig, "http://localhost:16666");
    }

    @Test
    void testCreatesOverriddenService() {
        BasicAuthenticationDetailsProvider authProvider = new TestBasicAuthenticationDetailsProvider();

        DataStoreConfig config = dataStoreConfig(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "remote-store",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.data-stores.0.service.auth.type", "OVERRIDDEN"
        ), "remote-store", Optional.of(authProvider));

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        AuthDetailsConfig.OverriddenAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.OverriddenAuthDetailsConfig.class,
                                 dataStoreConfig.getAuthDetailsConfig());
        assertSame(authProvider, authConfig.getAuthProviderOverride());
        assertNull(dataStoreConfig.getRegistryConfig());
    }

    @Test
    void testCreatesOverriddenServiceWithTls() {
        BasicAuthenticationDetailsProvider authProvider = new TestBasicAuthenticationDetailsProvider();

        DataStoreConfig config = dataStoreConfig(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.type", "OVERRIDDEN"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.cert-reload-duration", "PT5M"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.cert-ssl-algorithm", "SunX509")
        ), "remote-store", Optional.of(authProvider));

        KaasStoreConfig dataStoreConfig = assertInstanceOf(KaasStoreConfig.class, config);
        AuthDetailsConfig.OverriddenAuthDetailsConfig authConfig =
                assertInstanceOf(AuthDetailsConfig.OverriddenAuthDetailsConfig.class,
                                 dataStoreConfig.getAuthDetailsConfig());
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

        dataStoreConfig(Map.of(
                "oci.kiev.data-stores.0.store-name", "store",
                "oci.kiev.data-stores.0.app-name", "app"
        ), "store", authProvider);

        dataStoreConfig(Map.of(
                "oci.kiev.data-stores.0.backend", "DIRECT_DB",
                "oci.kiev.data-stores.0.store-name", "pdbdev",
                "oci.kiev.data-stores.0.app-name", "KievTest",
                "oci.kiev.data-stores.0.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev",
                "oci.kiev.data-stores.0.direct-db.user-name", "helidon",
                "oci.kiev.data-stores.0.direct-db.password", "changeit"
        ), "pdbdev", authProvider);

        dataStoreConfig(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem")
        ), "remote-store", authProvider);

        dataStoreConfig(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.type", "S2S"),
                Map.entry("oci.kiev.data-stores.0.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.tenant-id", "ocid1.tenancy.oc1..example")
        ), "remote-store", authProvider);
    }

    @Test
    void testCreatesMultipleDataStores() {
        KievDataStores dataStores = dataStores(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.store-name", "primary-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "primary-app"),
                Map.entry("oci.kiev.data-stores.1.backend", "DIRECT_DB"),
                Map.entry("oci.kiev.data-stores.1.store-name", "secondary-store"),
                Map.entry("oci.kiev.data-stores.1.app-name", "secondary-app"),
                Map.entry("oci.kiev.data-stores.1.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev"),
                Map.entry("oci.kiev.data-stores.1.direct-db.user-name", "helidon"),
                Map.entry("oci.kiev.data-stores.1.direct-db.password", "changeit")
        ));

        DataStoreConfig config = dataStores.dataStoreConfig("secondary-store");

        DirectDbStoreConfig dataStoreConfig = assertInstanceOf(DirectDbStoreConfig.class, config);
        assertEquals(2, dataStores.storeNames().size());
        assertEquals("primary-store", dataStores.storeConfig("primary-store").storeName());
        assertEquals("secondary-app", dataStores.storeConfig("secondary-store").appName());
        assertEquals("secondary-store", dataStoreConfig.getStoreName());
        assertEquals("secondary-app", dataStoreConfig.getAppName());
        assertEquals("jdbc:oracle:thin:@//localhost:1521/pdbdev", dataStoreConfig.getJDBCURL());
    }

    @Test
    void testCreatesSingleDataStore() {
        KievDataStores dataStores = dataStores(Map.of(
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app"
        ));

        DataStoreConfig config = dataStores.dataStoreConfig("primary-store");

        InMemoryDataStoreConfig dataStoreConfig = assertInstanceOf(InMemoryDataStoreConfig.class, config);
        assertEquals("primary-app", dataStores.storeConfig("primary-store").appName());
        assertEquals("primary-store", dataStoreConfig.getStoreName());
        assertEquals("primary-app", dataStoreConfig.getAppName());
    }

    @Test
    void testExposesNamedDataStoreServices() {
        KievDataStores dataStores = multipleDataStores();

        try {
            List<Service.QualifiedInstance<DataStore>> dataStoreServices =
                    new KievDataStoreFactory(dataStores).list(namedLookup(Service.Named.WILDCARD_NAME));
            List<Service.QualifiedInstance<MappedDataStore>> mappedDataStoreServices =
                    new KievMappedDataStoreFactory(dataStores).list(namedLookup(Service.Named.WILDCARD_NAME));
            List<Service.QualifiedInstance<KievTransactionSupport>> transactionSupportServices =
                    new KievTransactionSupportFactory(dataStores).list(namedLookup(Service.Named.WILDCARD_NAME));

            assertEquals(2, dataStoreServices.size());
            assertEquals(2, mappedDataStoreServices.size());
            assertEquals(2, transactionSupportServices.size());
            assertNotNull(namedService(dataStoreServices, "primary-store"));
            assertNotNull(namedService(dataStoreServices, "secondary-store"));
            assertNotNull(namedService(mappedDataStoreServices, "primary-store"));
            assertNotNull(namedService(mappedDataStoreServices, "secondary-store"));
            assertNotNull(namedService(transactionSupportServices, "primary-store"));
            assertNotNull(namedService(transactionSupportServices, "secondary-store"));
        } finally {
            dataStores.closeDataStores();
        }
    }

    @Test
    void testExposesSpecificNamedDataStoreServices() {
        KievDataStores dataStores = multipleDataStores();

        try {
            List<Service.QualifiedInstance<DataStore>> dataStoreServices =
                    new KievDataStoreFactory(dataStores).list(namedLookup("secondary-store"));
            List<Service.QualifiedInstance<MappedDataStore>> mappedDataStoreServices =
                    new KievMappedDataStoreFactory(dataStores).list(namedLookup("secondary-store"));
            List<Service.QualifiedInstance<KievTransactionSupport>> transactionSupportServices =
                    new KievTransactionSupportFactory(dataStores).list(namedLookup("secondary-store"));

            assertEquals(1, dataStoreServices.size());
            assertEquals(1, mappedDataStoreServices.size());
            assertEquals(1, transactionSupportServices.size());
            assertSame(dataStores.dataStore("secondary-store"), namedService(dataStoreServices, "secondary-store"));
            assertSame(dataStores.mappedDataStore("secondary-store"),
                       namedService(mappedDataStoreServices, "secondary-store"));
            assertSame(dataStores.transactionSupport("secondary-store"),
                       namedService(transactionSupportServices, "secondary-store"));
        } finally {
            dataStores.closeDataStores();
        }
    }

    @Test
    void testFailsUnqualifiedDataStoreServicesWhenSingleDataStoreConfigured() {
        KievDataStores dataStores = singleDataStore();

        try {
            assertUnqualifiedInjectionFails(dataStores, "primary-store");
        } finally {
            dataStores.closeDataStores();
        }
    }

    @Test
    void testFailsUnqualifiedDataStoreServicesWhenMultipleDataStoresConfigured() {
        KievDataStores dataStores = multipleDataStores();

        try {
            assertUnqualifiedInjectionFails(dataStores, "primary-store, secondary-store");
        } finally {
            dataStores.closeDataStores();
        }
    }

    @Test
    void testClosesAllDataStoresWhenCloseFails() {
        List<String> closed = new ArrayList<>();
        RuntimeException firstFailure = new RuntimeException("first");
        RuntimeException secondFailure = new RuntimeException("second");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                                                () -> KievDataStores.closeDataStores(List.of(
                                                        closeableDataStore(() -> closed.add("first")),
                                                        closeableDataStore(() -> {
                                                            closed.add("second");
                                                            throw firstFailure;
                                                        }),
                                                        closeableDataStore(() -> {
                                                            closed.add("third");
                                                            throw secondFailure;
                                                        }),
                                                        closeableDataStore(() -> closed.add("fourth")))));

        assertEquals(List.of("first", "second", "third", "fourth"), closed);
        assertEquals("Failed to close Kiev data stores", ex.getMessage());
        assertSame(firstFailure, ex.getCause());
        assertEquals(1, ex.getSuppressed().length);
        assertSame(secondFailure, ex.getSuppressed()[0]);
    }

    @Test
    void testFailsWithoutDataStores() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.of()));

        assertEquals("oci.kiev.data-stores must contain at least one data store", ex.getMessage());
    }

    @Test
    void testFailsWhenStoreNamesAreDuplicated() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.of(
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app",
                "oci.kiev.data-stores.1.store-name", "primary-store",
                "oci.kiev.data-stores.1.app-name", "duplicate-app"
        )));

        assertEquals("Duplicate Kiev data store configured with store-name 'primary-store'", ex.getMessage());
    }

    @Test
    void testFailsWithoutDirectDbConfig() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.of(
                "oci.kiev.data-stores.0.backend", "DIRECT_DB",
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app"
        )));

        assertEquals("oci.kiev.data-stores[].direct-db must be configured for DIRECT_DB backend "
                             + "for store-name 'primary-store'",
                     ex.getMessage());
    }

    @Test
    void testFailsWithoutServiceConfig() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app"
        )));

        assertEquals("oci.kiev.data-stores[].service must be configured for SERVICE backend "
                             + "for store-name 'primary-store'",
                     ex.getMessage());
    }

    @Test
    void testFailsWithoutS2sAuthEndpoint() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.backend", "SERVICE"),
                Map.entry("oci.kiev.data-stores.0.store-name", "remote-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "StoreApp"),
                Map.entry("oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example"),
                Map.entry("oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example"),
                Map.entry("oci.kiev.data-stores.0.service.auth.type", "S2S"),
                Map.entry("oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.data-stores.0.service.auth.s2s.tenant-id", "ocid1.tenancy.oc1..example")
        )));

        assertEquals("oci.kiev.data-stores[].service.auth.auth-endpoint must be configured "
                             + "for store-name 'remote-store'",
                     ex.getMessage());
    }

    @Test
    void testFailsWithoutS2sSection() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "remote-store",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.data-stores.0.service.auth.type", "S2S",
                "oci.kiev.data-stores.0.service.auth.auth-endpoint", "https://auth.example",
                "oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"
        )));

        assertEquals("oci.kiev.data-stores[].service.auth.s2s must be configured "
                             + "for store-name 'remote-store'",
                     ex.getMessage());
    }

    @Test
    void testFailsWithoutInstanceTls() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "remote-store",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.data-stores.0.service.auth.type", "INSTANCE"
        )));

        assertEquals("oci.kiev.data-stores[].service.auth.tls must be configured "
                             + "for store-name 'remote-store'",
                     ex.getMessage());
    }

    @Test
    void testFailsWithoutOverriddenAuthProvider() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dataStores(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "remote-store",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.data-stores.0.service.auth.type", "OVERRIDDEN"
        )));

        assertEquals("oci.kiev.data-stores[].service.auth.type=OVERRIDDEN requires "
                             + "BasicAuthenticationDetailsProvider to be available "
                             + "for store-name 'remote-store'",
                     ex.getMessage());
    }

    private static DataStoreConfig dataStoreConfig(Map<String, String> values, String storeName) {
        return dataStoreConfig(values, storeName, Optional.empty());
    }

    private static DataStoreConfig dataStoreConfig(Map<String, String> values,
                                                   String storeName,
                                                   Optional<BasicAuthenticationDetailsProvider> authProvider) {
        return dataStores(values, authProvider).dataStoreConfig(storeName);
    }

    private static DataStoreConfig dataStoreConfig(Map<String, String> values,
                                                   String storeName,
                                                   Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider) {
        return dataStores(values, authProvider).dataStoreConfig(storeName);
    }

    private static KievDataStores dataStores(Map<String, String> values) {
        return dataStores(values, Optional.empty());
    }

    private static KievDataStores dataStores(Map<String, String> values,
                                             Optional<BasicAuthenticationDetailsProvider> authProvider) {
        return dataStores(values, () -> authProvider);
    }

    private static KievDataStores dataStores(Map<String, String> values,
                                             Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider) {
        Config config = Config.just(ConfigSources.create(values));
        KievConfig kievConfig = new KievConfigFactory(config).get();
        return new KievDataStores(kievConfig, authProvider, new KievTransactions());
    }

    private static KievDataStores singleDataStore() {
        return dataStores(Map.of(
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app"
        ));
    }

    private static KievDataStores multipleDataStores() {
        return dataStores(Map.of(
                "oci.kiev.data-stores.0.store-name", "primary-store",
                "oci.kiev.data-stores.0.app-name", "primary-app",
                "oci.kiev.data-stores.1.store-name", "secondary-store",
                "oci.kiev.data-stores.1.app-name", "secondary-app"
        ));
    }

    private static Lookup unqualifiedLookup() {
        return Lookup.builder().build();
    }

    private static Lookup namedLookup(String storeName) {
        return Lookup.builder()
                .addQualifier(Qualifier.createNamed(storeName))
                .build();
    }

    private static DataStore closeableDataStore(Runnable closeAction) {
        return (DataStore) Proxy.newProxyInstance(DataStore.class.getClassLoader(),
                                                  new Class<?>[] {DataStore.class},
                                                  (proxy, method, args) -> {
                                                      if ("close".equals(method.getName())
                                                              && method.getParameterCount() == 0) {
                                                          closeAction.run();
                                                          return null;
                                                      }
                                                      throw new UnsupportedOperationException(method.getName());
                                                  });
    }

    private static void assertUnqualifiedInjectionFails(KievDataStores dataStores, String registeredNames) {
        IllegalStateException dataStoreFailure =
                assertThrows(IllegalStateException.class,
                             () -> new KievDataStoreFactory(dataStores).first(unqualifiedLookup()));
        IllegalStateException mappedDataStoreFailure =
                assertThrows(IllegalStateException.class,
                             () -> new KievMappedDataStoreFactory(dataStores).first(unqualifiedLookup()));
        IllegalStateException transactionSupportFailure =
                assertThrows(IllegalStateException.class,
                             () -> new KievTransactionSupportFactory(dataStores).first(unqualifiedLookup()));

        assertEquals("Kiev DataStore injection requires @Service.Named to select a configured data store. "
                             + "Registered store names: " + registeredNames
                             + ". Add @Service.Named with one of these names.",
                     dataStoreFailure.getMessage());
        assertEquals("Kiev MappedDataStore injection requires @Service.Named to select a configured data store. "
                             + "Registered store names: " + registeredNames
                             + ". Add @Service.Named with one of these names.",
                     mappedDataStoreFailure.getMessage());
        assertEquals("Kiev transaction support injection requires @Service.Named to select a configured data store. "
                             + "Registered store names: " + registeredNames
                             + ". Add @Service.Named with one of these names.",
                     transactionSupportFailure.getMessage());
    }

    private static void assertUsesFactorySslConfigurator(ClientConfigurator clientConfigurator) {
        CompositeClientConfigurator composite = assertInstanceOf(CompositeClientConfigurator.class, clientConfigurator);
        assertEquals(2, composite.getConfigurators().size());
        assertInstanceOf(SslTrustStoreConfigurator.class, composite.getConfigurators().get(1));
    }

    private static void assertRegistryDisabled(KaasStoreConfig dataStoreConfig, String endpointOverride) {
        ClientRegistryConfig registryConfig = dataStoreConfig.getRegistryConfig();
        assertNotNull(registryConfig);
        assertEquals(Boolean.FALSE, registryConfig.getEnabled());
        assertEquals(endpointOverride, registryConfig.getEndpointOverride());
    }

    private static <T> T namedService(List<Service.QualifiedInstance<T>> services, String storeName) {
        Qualifier qualifier = Qualifier.createNamed(storeName);
        return services.stream()
                .filter(service -> service.qualifiers().contains(qualifier))
                .findFirst()
                .map(Service.QualifiedInstance::get)
                .orElseThrow();
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
