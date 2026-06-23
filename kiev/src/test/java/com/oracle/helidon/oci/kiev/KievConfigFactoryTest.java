/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KievConfigFactoryTest {

    @Test
    void testLoadsEmptyRootConfig() {
        KievConfig config = new KievConfigFactory(config(Map.of())).get();

        assertTrue(config.dataStores().isEmpty());
    }

    @Test
    void testLoadsInMemoryDefaults() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.data-stores.0.store-name", "test-store",
                "oci.kiev.data-stores.0.app-name", "test-app"
        ))).get();

        KievStoreConfig storeConfig = config.dataStores().get(0);
        assertEquals(KievBackend.IN_MEMORY, storeConfig.backend());
        assertEquals("test-store", storeConfig.storeName());
        assertEquals("test-app", storeConfig.appName());
        assertEquals(100, storeConfig.transactionMaxReads());
        assertEquals(100, storeConfig.transactionMaxWrites());
        assertTrue(storeConfig.directDb().isEmpty());
        assertTrue(storeConfig.service().isEmpty());
        assertFalse(storeConfig.streamDeletedColumnValues());
    }

    @Test
    void testLoadsDirectDb() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.data-stores.0.backend", "DIRECT_DB",
                "oci.kiev.data-stores.0.store-name", "pdbdev",
                "oci.kiev.data-stores.0.app-name", "KievTest",
                "oci.kiev.data-stores.0.transaction-max-reads", "111",
                "oci.kiev.data-stores.0.transaction-max-writes", "222",
                "oci.kiev.data-stores.0.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev",
                "oci.kiev.data-stores.0.direct-db.user-name", "helidon",
                "oci.kiev.data-stores.0.direct-db.password", "changeit",
                "oci.kiev.data-stores.0.direct-db.schema-name", "helidon"
        ))).get();

        KievStoreConfig storeConfig = config.dataStores().get(0);
        assertEquals(KievBackend.DIRECT_DB, storeConfig.backend());
        assertEquals("pdbdev", storeConfig.storeName());
        assertEquals("KievTest", storeConfig.appName());
        assertEquals(111, storeConfig.transactionMaxReads());
        assertEquals(222, storeConfig.transactionMaxWrites());
        assertTrue(storeConfig.directDb().isPresent());
        assertEquals("jdbc:oracle:thin:@//localhost:1521/pdbdev", storeConfig.directDb().get().jdbcUrl());
        assertEquals("helidon", storeConfig.directDb().get().userName());
        assertEquals("changeit", storeConfig.directDb().get().password());
        assertEquals("helidon", storeConfig.directDb().get().schemaName().orElseThrow());
        assertTrue(storeConfig.service().isEmpty());
    }

    @Test
    void testLoadsMultipleDataStores() {
        KievConfig config = new KievConfigFactory(config(Map.ofEntries(
                Map.entry("oci.kiev.data-stores.0.store-name", "primary-store"),
                Map.entry("oci.kiev.data-stores.0.app-name", "primary-app"),
                Map.entry("oci.kiev.data-stores.1.backend", "DIRECT_DB"),
                Map.entry("oci.kiev.data-stores.1.store-name", "secondary-store"),
                Map.entry("oci.kiev.data-stores.1.app-name", "secondary-app"),
                Map.entry("oci.kiev.data-stores.1.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev"),
                Map.entry("oci.kiev.data-stores.1.direct-db.user-name", "helidon"),
                Map.entry("oci.kiev.data-stores.1.direct-db.password", "changeit")
        ))).get();

        assertEquals(2, config.dataStores().size());
        assertEquals("primary-store", config.dataStores().get(0).storeName());
        assertEquals(KievBackend.IN_MEMORY, config.dataStores().get(0).backend());
        assertEquals("secondary-store", config.dataStores().get(1).storeName());
        assertEquals(KievBackend.DIRECT_DB, config.dataStores().get(1).backend());
        assertTrue(config.dataStores().get(1).directDb().isPresent());
    }

    @Test
    void testLoadsServiceDefaults() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"
        ))).get();

        KievServiceConfig serviceConfig = config.dataStores().get(0).service().orElseThrow();
        KievServiceAuthConfig authConfig = serviceConfig.auth().orElseThrow();
        KievServiceTlsConfig tlsConfig = authConfig.tls().orElseThrow();
        assertTrue(serviceConfig.locality().isEmpty());
        assertEquals("ocid1.compartment.oc1..example", serviceConfig.compartmentId());
        assertEquals("http://localhost:16666", serviceConfig.frontendEndpoint());
        assertEquals(KievAuthType.INSTANCE, authConfig.type());
        assertEquals("/tmp/root.pem", tlsConfig.rootCertPemPath().orElseThrow());
        assertFalse(authConfig.authEndpoint().isPresent());
    }

    @Test
    void testLoadsStreamDeletedColumnValues() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem",
                "oci.kiev.data-stores.0.stream-deleted-column-values", "true"
        ))).get();

        assertTrue(config.dataStores().get(0).streamDeletedColumnValues());
    }

    @Test
    void testDefaultsServiceLocalityFromOciEnv() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.env.ad-number", "ad2",
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem"
        ))).get();

        KievServiceConfig serviceConfig = config.dataStores().get(0).service().orElseThrow();
        assertEquals(ClientRegistryLocality.AD2, serviceConfig.locality().orElseThrow());
    }

    @Test
    void testExplicitServiceLocalityBuildsWithoutConfigRoot() {
        KievServiceConfig configuredService = KievServiceConfig.builder()
                .compartmentId("ocid1.compartment.oc1..example")
                .frontendEndpoint("http://localhost:16666")
                .locality(ClientRegistryLocality.AD1)
                .buildPrototype();
        KievStoreConfig configuredStore = KievStoreConfig.builder()
                .backend(KievBackend.SERVICE)
                .storeName("kaaspdb")
                .appName("StoreApp")
                .service(configuredService)
                .buildPrototype();
        KievConfig config = KievConfig.builder()
                .dataStores(List.of(configuredStore))
                .buildPrototype();

        KievServiceConfig serviceConfig = config.dataStores().get(0).service().orElseThrow();
        assertEquals(ClientRegistryLocality.AD1, serviceConfig.locality().orElseThrow());
    }

    @Test
    void testLoadsRootCertPathAlias() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.data-stores.0.service.auth.tls.root-cert-path", "/tmp/root.pem"
        ))).get();

        KievServiceTlsConfig tlsConfig = config.dataStores()
                .get(0)
                .service()
                .orElseThrow()
                .auth()
                .orElseThrow()
                .tls()
                .orElseThrow();
        assertEquals("/tmp/root.pem", tlsConfig.rootCertPemPath().orElseThrow());
    }

    @Test
    void failWhenRootCertPemPathAndRootCertPathAreConfigured() {
        Config config = config(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.data-stores.0.service.auth.tls.root-cert-pem-path", "/tmp/root.pem",
                "oci.kiev.data-stores.0.service.auth.tls.root-cert-path", "/tmp/root-alias.pem"
        ));

        assertThrows(IllegalArgumentException.class, () -> new KievConfigFactory(config).get());
    }

    @Test
    void testLoadsKiabLocalAuth() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.data-stores.0.service.auth.type", "KIAB_LOCAL"
        ))).get();

        assertEquals(KievAuthType.KIAB_LOCAL,
                     config.dataStores().get(0).service().orElseThrow().auth().orElseThrow().type());
    }

    @Test
    void testLoadsOverriddenAuth() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.data-stores.0.backend", "SERVICE",
                "oci.kiev.data-stores.0.store-name", "kaaspdb",
                "oci.kiev.data-stores.0.app-name", "StoreApp",
                "oci.kiev.data-stores.0.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.data-stores.0.service.frontend-endpoint", "https://frontend.example",
                "oci.kiev.data-stores.0.service.auth.type", "OVERRIDDEN"
        ))).get();

        assertEquals(KievAuthType.OVERRIDDEN,
                     config.dataStores().get(0).service().orElseThrow().auth().orElseThrow().type());
    }

    @Test
    void testLoadsS2sAuth() {
        KievConfig config = new KievConfigFactory(config(Map.ofEntries(
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
        ))).get();

        KievServiceConfig serviceConfig = config.dataStores().get(0).service().orElseThrow();
        KievServiceAuthConfig authConfig = serviceConfig.auth().orElseThrow();
        KievServiceS2sConfig s2sConfig = authConfig.s2s().orElseThrow();
        KievServiceTlsConfig tlsConfig = authConfig.tls().orElseThrow();
        assertEquals(ClientRegistryLocality.AD1, serviceConfig.locality().orElseThrow());
        assertEquals(KievAuthType.S2S, authConfig.type());
        assertEquals("https://auth.example", authConfig.authEndpoint().orElseThrow());
        assertEquals("/tmp/root.pem", tlsConfig.rootCertPemPath().orElseThrow());
        assertEquals("/tmp/leaf.pem", s2sConfig.leafCertPath().orElseThrow());
        assertEquals("/tmp/leaf.key", s2sConfig.leafCertKeyPath().orElseThrow());
        assertEquals("/tmp/intermediate.pem", s2sConfig.intermediateCertPath().orElseThrow());
        assertEquals("ocid1.tenancy.oc1..example", s2sConfig.tenantId().orElseThrow());
        assertEquals("secret", s2sConfig.keyPassphrase().orElseThrow());
        assertEquals(Duration.ofMinutes(15), tlsConfig.certReloadDuration().orElseThrow());
        assertEquals("SunX509", tlsConfig.certSslAlgorithm().orElseThrow());
    }

    private static Config config(Map<String, String> values) {
        return Config.just(ConfigSources.create(values));
    }
}
