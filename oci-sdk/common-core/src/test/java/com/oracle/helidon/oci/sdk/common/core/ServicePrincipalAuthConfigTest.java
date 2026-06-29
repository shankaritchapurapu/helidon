/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import java.net.URI;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicePrincipalAuthConfigTest {

    @Test
    void bindsServicePrincipalAuthConfig() {
        ServicePrincipalAuthConfig config = create(Map.ofEntries(
                Map.entry("service-principal.federation-endpoint", "https://auth.example/v1/x509"),
                Map.entry("service-principal.tenant-id", "ocid1.tenancy.oc1..test"),
                Map.entry("service-principal.imds-base-uri", "http://127.0.0.1:8000/opc/v2/"),
                Map.entry("service-principal.use-platform-provided", "false"),
                Map.entry("service-principal.certificates.0.certificate", "/tmp/leaf.pem"),
                Map.entry("service-principal.certificates.0.private-key", "/tmp/leaf.key"),
                Map.entry("service-principal.certificates.0.passphrase", ""),
                Map.entry("service-principal.certificates.1.certificate", "/tmp/intermediate.pem")
        ));

        assertEquals(URI.create("https://auth.example/v1/x509"), config.federationEndpoint().orElseThrow());
        assertEquals("ocid1.tenancy.oc1..test", config.tenantId().orElseThrow());
        assertEquals(URI.create("http://127.0.0.1:8000/opc/v2/"), config.imdsBaseUri().orElseThrow());
        assertFalse(config.usePlatformProvided());
        assertEquals(2, config.certificates().size());
        assertEquals("/tmp/leaf.pem", config.certificates().getFirst().certificate());
        assertEquals("/tmp/leaf.key", config.certificates().getFirst().privateKey().orElseThrow());
        assertEquals("/tmp/intermediate.pem", config.certificates().get(1).certificate());
        assertTrue(config.certificates().get(1).privateKey().isEmpty());
    }

    @Test
    void defaultsToPlatformProvidedServicePrincipalConfig() {
        ServicePrincipalAuthConfig config = create(Map.of());

        assertTrue(config.usePlatformProvided());
        assertTrue(config.certificates().isEmpty());
        assertTrue(config.federationEndpoint().isEmpty());
        assertTrue(config.tenantId().isEmpty());
        assertTrue(config.imdsBaseUri().isEmpty());
    }

    @Test
    void bindsPlatformProvidedServicePrincipalConfig() {
        ServicePrincipalAuthConfig config = create(Map.of(
                "service-principal.use-platform-provided", "true"
        ));

        assertTrue(config.usePlatformProvided());
    }

    private static ServicePrincipalAuthConfig create(Map<String, String> values) {
        Config root = Config.just(ConfigSources.create(values));
        return ServicePrincipalAuthConfig.create(root.get("service-principal"));
    }
}
