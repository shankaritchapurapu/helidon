/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.service.registry.Services;

import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatorClientFactoryTest extends BaseAuthenticationClientTest {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        if (!INITIALIZED.get()) {
            ServiceAuthenticationClient serviceAuthClient = serviceAuthenticationClient();
            assertThat(serviceAuthClient, notNullValue());
            Services.set(ServiceAuthenticationClient.class, serviceAuthClient);
            INITIALIZED.set(true);
        }
    }

    @Test
    void testClientCreation() {
        AuthenticatorClientFactory factory = new AuthenticatorClientFactory(
                identityConfigFactory(), ociEnvLocationDefaults(failingDefaultRegion()));
        AuthenticatorClient client = factory.get();
        assertThat(client, notNullValue());
    }

    @Test
    void testClientWithUri() {
        AuthenticationConfig authenticationConfig = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .serviceUri(java.net.URI.create("https://auth.us-phoenix-1.oraclecloud.com"))
                .useInstancePrincipal(false)
                .build();

        AuthenticatorClientFactory factory = new AuthenticatorClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(failingDefaultRegion()));
        AuthenticatorClient client = factory.get();
        assertThat(client, notNullValue());
    }

    @Test
    void testClientWithDefaultRegion() {
        AuthenticationConfig authenticationConfig = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .useInstancePrincipal(false)
                .build();

        AtomicBoolean defaultRegionCalled = new AtomicBoolean();
        AuthenticatorClientFactory factory = new AuthenticatorClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(defaultRegion(defaultRegionCalled)));
        AuthenticatorClient client = factory.get();
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

        AuthenticatorClientFactory factory = new AuthenticatorClientFactory(
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

        AuthenticatorClientFactory factory = new AuthenticatorClientFactory(
                identityConfigFactory(authenticationConfig, authorizationConfig()),
                ociEnvLocationDefaults(emptyDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }
}
