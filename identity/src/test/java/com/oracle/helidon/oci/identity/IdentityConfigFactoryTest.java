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
import static java.util.Map.entry;

class IdentityConfigFactoryTest {

    @Test
    void testConfigBinding() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("oci.identity.authentication.global-business-unit", "gbu"),
                entry("oci.identity.authentication.team-name", "team"),
                entry("oci.identity.authentication.application-name", "app"),
                entry("oci.identity.authentication.service-uri", "https://auth.us-phoenix-1.oraclecloud.com"),
                entry("oci.identity.authentication.use-instance-principal", "false"),
                entry("oci.identity.authorization.enabled", "true"),
                entry("oci.identity.authorization.service-name", "service"),
                entry("oci.identity.authorization.service-uri", "https://authservice.svc.ad1.us-phoenix-1"),
                entry("oci.identity.authorization.service-enclave", "true"),
                entry("oci.identity.splat-aware.splat-request-port", "8443"),
                entry("oci.identity.splat-aware.skip-authorization-for-splat", "true"),
                entry("oci.identity.splat-aware.validate-splat-cert", "true"),
                entry("oci.identity.splat-aware.disable-tag-only-request-check", "true"),
                entry("oci.identity.splat-aware.reject-x-region-calls", "true"),
                entry("oci.identity.splat-aware.region", "us-ashburn-1")
        )));

        IdentityConfig identityConfig = new IdentityConfigFactory(config).get();

        assertThat(identityConfig.authentication().serviceUri().orElseThrow(),
                   is(URI.create("https://auth.us-phoenix-1.oraclecloud.com")));
        assertThat(identityConfig.authentication().region().isEmpty(), is(true));
        assertThat(identityConfig.authorization().serviceUri().orElseThrow(),
                   is(URI.create("https://authservice.svc.ad1.us-phoenix-1")));
        assertThat(identityConfig.authorization().serviceEnclave(), is(true));
        assertThat(identityConfig.splatAware().splatRequestPort(), is(8443));
        assertThat(identityConfig.splatAware().skipAuthorizationForSplat(), is(true));
        assertThat(identityConfig.splatAware().validateSplatCert(), is(true));
        assertThat(identityConfig.splatAware().disableTagOnlyRequestCheck(), is(true));
        assertThat(identityConfig.splatAware().rejectXRegionCalls(), is(true));
        assertThat(identityConfig.splatAware().region().orElseThrow(), is("us-ashburn-1"));
    }
}
