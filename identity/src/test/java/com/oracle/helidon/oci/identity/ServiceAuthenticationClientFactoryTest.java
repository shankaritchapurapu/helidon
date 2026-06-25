/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceAuthenticationClientFactoryTest extends BaseAuthenticationClientTest {

    @Test
    void testClientCreation() {
        ServiceAuthenticationClient client = serviceAuthenticationClient();
        assertThat(client, notNullValue());
    }

    @Test
    void testClientWithUri() {
        AuthenticationConfig authenticationConfig = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .serviceUri(java.net.URI.create("https://auth.us-phoenix-1.oraclecloud.com"))
                .rootCertPath(testRootCertPath())
                .useInstancePrincipal(false)
                .certificates(java.util.List.of(AuthCertificateConfig.builder()
                                                   .certificate("serverCert.pem")
                                                   .privateKey("serverKey.pem")
                                                   .build()))
                .build();

        ServiceAuthenticationClient client = new ServiceAuthenticationClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(failingDefaultRegion())).get();
        assertThat(client, notNullValue());
    }

    @Test
    void testClientWithDefaultRegion() {
        AuthenticationConfig authenticationConfig = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .rootCertPath(testRootCertPath())
                .useInstancePrincipal(false)
                .certificates(java.util.List.of(AuthCertificateConfig.builder()
                                                   .certificate("serverCert.pem")
                                                   .privateKey("serverKey.pem")
                                                   .build()))
                .build();

        AtomicBoolean defaultRegionCalled = new AtomicBoolean();
        ServiceAuthenticationClient client = new ServiceAuthenticationClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(defaultRegion(defaultRegionCalled))).get();
        assertThat(client, notNullValue());
        assertThat(defaultRegionCalled.get(), is(true));
    }

    @Test
    void testRejectsRegionAndUri() {
        AuthenticationConfig authenticationConfig = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .region("us-phoenix-1")
                .serviceUri(java.net.URI.create("https://auth.us-phoenix-1.oraclecloud.com"))
                .useInstancePrincipal(false)
                .build();

        ServiceAuthenticationClientFactory factory = new ServiceAuthenticationClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(failingDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }

    @Test
    void testRejectsMissingRegionAndDefault() {
        AuthenticationConfig authenticationConfig = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .useInstancePrincipal(false)
                .build();

        ServiceAuthenticationClientFactory factory = new ServiceAuthenticationClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(emptyDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }

    @Test
    void testRejectsCertModeWithoutCerts() {
        AuthenticationConfig authenticationConfig = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .region("us-phoenix-1")
                .useInstancePrincipal(false)
                .build();

        ServiceAuthenticationClientFactory factory = new ServiceAuthenticationClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(failingDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }
}
