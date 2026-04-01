/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.time.Duration;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

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
                Map.entry("oci.kiev.service.auth.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.service.auth.auth-endpoint", "https://auth.example"),
                Map.entry("oci.kiev.service.auth.cert-reload-duration", "PT5M"),
                Map.entry("oci.kiev.service.auth.cert-ssl-algorithm", "SunX509")
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
                Map.entry("oci.kiev.service.auth.root-cert-pem-path", "/tmp/root.pem"),
                Map.entry("oci.kiev.service.auth.leaf-cert-path", "/tmp/leaf.pem"),
                Map.entry("oci.kiev.service.auth.leaf-cert-key-path", "/tmp/leaf.key"),
                Map.entry("oci.kiev.service.auth.intermediate-cert-path", "/tmp/intermediate.pem"),
                Map.entry("oci.kiev.service.auth.tenant-id", "ocid1.tenancy.oc1..example"),
                Map.entry("oci.kiev.service.auth.key-passphrase", "secret"),
                Map.entry("oci.kiev.service.auth.cert-reload-duration", "PT15M"),
                Map.entry("oci.kiev.service.auth.cert-ssl-algorithm", "SunX509")
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
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "remote-store",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.service.auth.type", "S2S"
        )).get());

        assertEquals("oci.kiev.service.auth.auth-endpoint must be configured", ex.getMessage());
    }

    private static KievDataStoreConfigFactory factory(Map<String, String> values) {
        Config config = Config.just(ConfigSources.create(values));
        KievConfig kievConfig = new KievConfigFactory(config).get();
        return new KievDataStoreConfigFactory(kievConfig);
    }
}
