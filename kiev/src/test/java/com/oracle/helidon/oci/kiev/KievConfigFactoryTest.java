/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.time.Duration;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KievConfigFactoryTest {

    @Test
    void testLoadsDefaults() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.store-name", "test-store",
                "oci.kiev.app-name", "test-app"
        ))).get();

        assertEquals(KievBackend.IN_MEMORY, config.backend());
        assertEquals("test-store", config.storeName());
        assertEquals("test-app", config.appName());
        assertEquals(100, config.transactionMaxReads());
        assertEquals(100, config.transactionMaxWrites());
        assertTrue(config.directDb().isEmpty());
        assertTrue(config.service().isEmpty());
    }

    @Test
    void testLoadsDirectDb() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.backend", "DIRECT_DB",
                "oci.kiev.store-name", "pdbdev",
                "oci.kiev.app-name", "KievTest",
                "oci.kiev.transaction-max-reads", "111",
                "oci.kiev.transaction-max-writes", "222",
                "oci.kiev.direct-db.jdbc-url", "jdbc:oracle:thin:@//localhost:1521/pdbdev",
                "oci.kiev.direct-db.user-name", "helidon",
                "oci.kiev.direct-db.password", "changeit",
                "oci.kiev.direct-db.schema-name", "helidon"
        ))).get();

        assertEquals(KievBackend.DIRECT_DB, config.backend());
        assertEquals("pdbdev", config.storeName());
        assertEquals("KievTest", config.appName());
        assertEquals(111, config.transactionMaxReads());
        assertEquals(222, config.transactionMaxWrites());
        assertTrue(config.directDb().isPresent());
        assertEquals("jdbc:oracle:thin:@//localhost:1521/pdbdev", config.directDb().get().jdbcUrl());
        assertEquals("helidon", config.directDb().get().userName());
        assertEquals("changeit", config.directDb().get().password());
        assertEquals("helidon", config.directDb().get().schemaName().orElseThrow());
        assertTrue(config.service().isEmpty());
    }

    @Test
    void testLoadsServiceDefaults() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "kaaspdb",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.service.auth.root-cert-pem-path", "/tmp/root.pem"
        ))).get();

        assertEquals(KievBackend.SERVICE, config.backend());
        assertTrue(config.service().isPresent());
        assertEquals("ocid1.compartment.oc1..example", config.service().get().compartmentId());
        assertEquals("http://localhost:16666", config.service().get().frontendEndpoint());
        assertEquals(ClientRegistryLocality.REGIONAL, config.service().get().locality());
        assertTrue(config.service().get().auth().isPresent());
        assertEquals(KievAuthType.INSTANCE, config.service().get().auth().get().type());
        assertEquals("/tmp/root.pem", config.service().get().auth().get().rootCertPemPath().orElseThrow());
        assertFalse(config.service().get().auth().get().authEndpoint().isPresent());
    }

    @Test
    void testLoadsKiabLocalAuth() {
        KievConfig config = new KievConfigFactory(config(Map.of(
                "oci.kiev.backend", "SERVICE",
                "oci.kiev.store-name", "kaaspdb",
                "oci.kiev.app-name", "StoreApp",
                "oci.kiev.service.compartment-id", "ocid1.compartment.oc1..example",
                "oci.kiev.service.frontend-endpoint", "http://localhost:16666",
                "oci.kiev.service.auth.type", "KIAB_LOCAL"
        ))).get();

        assertEquals(KievAuthType.KIAB_LOCAL, config.service().orElseThrow().auth().orElseThrow().type());
    }

    @Test
    void testLoadsS2sAuth() {
        KievConfig config = new KievConfigFactory(config(Map.ofEntries(
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
        ))).get();

        KievServiceConfig serviceConfig = config.service().orElseThrow();
        KievServiceAuthConfig authConfig = serviceConfig.auth().orElseThrow();
        assertEquals(ClientRegistryLocality.AD1, serviceConfig.locality());
        assertEquals(KievAuthType.S2S, authConfig.type());
        assertEquals("https://auth.example", authConfig.authEndpoint().orElseThrow());
        assertEquals("/tmp/root.pem", authConfig.rootCertPemPath().orElseThrow());
        assertEquals("/tmp/leaf.pem", authConfig.leafCertPath().orElseThrow());
        assertEquals("/tmp/leaf.key", authConfig.leafCertKeyPath().orElseThrow());
        assertEquals("/tmp/intermediate.pem", authConfig.intermediateCertPath().orElseThrow());
        assertEquals("ocid1.tenancy.oc1..example", authConfig.tenantId().orElseThrow());
        assertEquals("secret", authConfig.keyPassphrase().orElseThrow());
        assertEquals(Duration.ofMinutes(15), authConfig.certReloadDuration().orElseThrow());
        assertEquals("SunX509", authConfig.certSslAlgorithm().orElseThrow());
    }

    private static Config config(Map<String, String> values) {
        return Config.just(ConfigSources.create(values));
    }
}
