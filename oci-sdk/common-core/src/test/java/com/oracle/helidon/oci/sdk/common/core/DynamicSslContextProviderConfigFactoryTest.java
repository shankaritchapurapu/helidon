/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.Qualifier;
import io.helidon.service.registry.Service;

import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSslContextProviderConfigFactoryTest {

    @Test
    void testLoadsNamedProvidersFromRootConfig() {
        DynamicSslContextProviderConfigFactory factory = factory(Map.ofEntries(
                Map.entry("oci.dynamic-ssl-context-providers.0.name", "workflow"),
                Map.entry("oci.dynamic-ssl-context-providers.0.leaf-cert-path", "/tmp/workflow-leaf.pem"),
                Map.entry("oci.dynamic-ssl-context-providers.0.leaf-cert-key-path", "/tmp/workflow-leaf.key"),
                Map.entry("oci.dynamic-ssl-context-providers.0.leaf-cert-key-passphrase", "secret"),
                Map.entry("oci.dynamic-ssl-context-providers.0.intermediate-cert-path",
                          "/tmp/workflow-intermediate.pem"),
                Map.entry("oci.dynamic-ssl-context-providers.0.root-cert-path", "/tmp/workflow-root.pem"),
                Map.entry("oci.dynamic-ssl-context-providers.0.duration", "PT15M"),
                Map.entry("oci.dynamic-ssl-context-providers.1.name", "kiev-service-auth"),
                Map.entry("oci.dynamic-ssl-context-providers.1.root-cert-pem-path", "/tmp/kiev-root.pem"),
                Map.entry("oci.dynamic-ssl-context-providers.1.cert-reload-duration", "PT5M"),
                Map.entry("oci.dynamic-ssl-context-providers.1.cert-ssl-algorithm", "SunX509")
        ));

        DynamicSslContextProviderConfig workflow = provider(factory.services(), "workflow");
        assertEquals("/tmp/workflow-leaf.pem", workflow.getLeafCertPath());
        assertEquals("/tmp/workflow-leaf.key", workflow.getLeafCertKeyPath());
        assertEquals("secret", workflow.getLeafCertKeyPassphrase());
        assertEquals("/tmp/workflow-intermediate.pem", workflow.getIntermediateCertPath());
        assertEquals("/tmp/workflow-root.pem", workflow.getRootCertPath());
        assertEquals(Duration.ofMinutes(15), workflow.getDuration());

        DynamicSslContextProviderConfig kiev = provider(factory.services(), "kiev-service-auth");
        assertEquals("/tmp/kiev-root.pem", kiev.getRootCertPath());
        assertEquals(Duration.ofMinutes(5), kiev.getDuration());
        assertEquals("SunX509", kiev.getSslAlgorithm());
    }

    @Test
    void testRejectsDuplicateProviderNames() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                                                () -> factory(Map.of(
                                                        "oci.dynamic-ssl-context-providers.0.name", "workflow",
                                                        "oci.dynamic-ssl-context-providers.0.root-cert-path",
                                                        "/tmp/a.pem",
                                                        "oci.dynamic-ssl-context-providers.1.name", "workflow",
                                                        "oci.dynamic-ssl-context-providers.1.root-cert-path",
                                                        "/tmp/b.pem"
                                                )));

        assertEquals("Duplicate dynamic SSL context provider configured with name 'workflow'", ex.getMessage());
    }

    @Test
    void testRejectsProviderWithoutName() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                                           () -> factory(Map.of(
                                                   "oci.dynamic-ssl-context-providers.0.root-cert-path",
                                                   "/tmp/root.pem"
                                           )));

        assertTrue(String.valueOf(ex.getMessage()).contains("name"));
    }

    @Test
    void testRejectsProviderWithBlankName() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                   () -> factory(Map.of(
                                                           "oci.dynamic-ssl-context-providers.0.name", "",
                                                           "oci.dynamic-ssl-context-providers.0.root-cert-path",
                                                           "/tmp/root.pem"
                                                   )));

        assertEquals("name must not be blank", ex.getMessage());
    }

    @Test
    void testRejectsProviderWithoutRootCertPath() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                                           () -> factory(Map.of(
                                                   "oci.dynamic-ssl-context-providers.0.name", "workflow"
                                           )));

        assertTrue(String.valueOf(ex.getMessage()).contains("rootCertPath"));
    }

    @Test
    void testRejectsProviderWithBlankRootCertPath() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                   () -> factory(Map.of(
                                                           "oci.dynamic-ssl-context-providers.0.name", "workflow",
                                                           "oci.dynamic-ssl-context-providers.0.root-cert-path", ""
                                                   )));

        assertEquals("root-cert-path must not be blank", ex.getMessage());
    }

    @Test
    void testExposesNamedProviderServices() {
        DynamicSslContextProviderConfigFactory factory = factory(Map.of(
                "oci.dynamic-ssl-context-providers.0.name", "workflow",
                "oci.dynamic-ssl-context-providers.0.root-cert-path", "/tmp/root.pem"
        ));
        List<Service.QualifiedInstance<DynamicSslContextProviderConfig>> services = factory.services();

        assertEquals(1, services.size());
        assertEquals("/tmp/root.pem", services.getFirst().get().getRootCertPath());
        assertNotNull(services.getFirst().qualifiers()
                              .stream()
                              .filter(Qualifier.createNamed("workflow")::equals)
                              .findFirst()
                              .orElse(null));
    }

    private static Config config(Map<String, String> values) {
        return Config.just(ConfigSources.create(values));
    }

    private static DynamicSslContextProviderConfigFactory factory(Map<String, String> values) {
        return new DynamicSslContextProviderConfigFactory(DynamicSslProvidersConfig.create(config(values).get("oci")));
    }

    private static DynamicSslContextProviderConfig provider(
            List<Service.QualifiedInstance<DynamicSslContextProviderConfig>> services,
            String name) {
        return services.stream()
                .filter(it -> it.qualifiers().contains(Qualifier.createNamed(name)))
                .findFirst()
                .orElseThrow()
                .get();
    }
}
