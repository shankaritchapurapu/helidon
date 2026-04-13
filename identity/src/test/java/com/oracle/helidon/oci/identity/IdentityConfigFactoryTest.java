/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class IdentityConfigFactoryTest {

    @Test
    void testConfigBinding() {
        Config config = Config.just(ConfigSources.create(Map.of(
                "oci.identity.authentication.global-business-unit", "gbu",
                "oci.identity.authentication.team-name", "team",
                "oci.identity.authentication.application-name", "app",
                "oci.identity.authentication.service-uri", "https://auth.us-phoenix-1.oraclecloud.com",
                "oci.identity.authentication.use-instance-principal", "false",
                "oci.identity.authentication.hard-coded-key-supplier", "true",
                "oci.identity.authorization.enabled", "true",
                "oci.identity.authorization.service-name", "service",
                "oci.identity.authorization.service-uri", "https://authservice.svc.ad1.us-phoenix-1",
                "oci.identity.authorization.service-enclave", "true"
        )));

        IdentityConfig identityConfig = new IdentityConfigFactory(config).get();

        assertThat(identityConfig.authentication().serviceUri().orElseThrow(),
                   is(URI.create("https://auth.us-phoenix-1.oraclecloud.com")));
        assertThat(identityConfig.authentication().region().isEmpty(), is(true));
        assertThat(identityConfig.authorization().serviceUri().orElseThrow(),
                   is(URI.create("https://authservice.svc.ad1.us-phoenix-1")));
        assertThat(identityConfig.authorization().serviceEnclave(), is(true));
    }
}
