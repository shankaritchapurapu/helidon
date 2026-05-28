/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import io.helidon.service.registry.Services;

import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationClientFactoryTest extends BaseAuthenticationClientTest {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        if (!INITIALIZED.get()) {
            ServiceAuthenticationClient serviceAuthClient = serviceAuthenticationClient(true);
            assertThat(serviceAuthClient, notNullValue());
            Services.set(ServiceAuthenticationClient.class, serviceAuthClient);
            INITIALIZED.set(true);
        }
    }

    @Test
    void testClientCreation() {
        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(), ociEnvLocationDefaults(failingDefaultRegion()));
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
    }

    @Test
    void testClientWithEnclaveUri() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .serviceEnclave(true)
                .serviceUri(java.net.URI.create("https://authservice.svc.ad1.us-phoenix-1"))
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(failingDefaultRegion()));
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
    }

    @Test
    void testClientWithDefaultRegion() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .physicalAd("AD-1")
                .build();

        AtomicBoolean defaultRegionCalled = new AtomicBoolean();
        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(defaultRegion(defaultRegionCalled)));
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
        assertThat(defaultRegionCalled.get(), is(true));
    }

    @Test
    void testClientWithExplicitNonEnclaveUriAndDefaultRegion() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .serviceUri(java.net.URI.create("https://auth.us-ashburn-1.oraclecloud.com"))
                .physicalAd("AD-1")
                .build();

        AtomicBoolean defaultRegionCalled = new AtomicBoolean();
        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(defaultRegion(defaultRegionCalled)));
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
        assertThat(defaultRegionCalled.get(), is(true));
    }

    @Test
    void testServiceEnclaveClientWithDefaultAvailabilityDomain() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .serviceEnclave(true)
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(ociEnvLocationConfig(), failingDefaultRegion()));
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
    }

    @Test
    void testServiceEnclaveClientWithExplicitAvailabilityDomain() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .serviceEnclave(true)
                .availabilityDomain("iad-ad-2")
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(failingDefaultRegion()));
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
    }

    @Test
    void testRejectsEnclaveUriWithExplicitAvailabilityDomain() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .serviceEnclave(true)
                .serviceUri(java.net.URI.create("https://authservice.svc.ad1.us-phoenix-1"))
                .availabilityDomain("iad-ad-1")
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(ociEnvLocationConfig(), failingDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }

    @Test
    void testRejectsEnclaveWithoutAd() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .serviceEnclave(true)
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(failingDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }

    @Test
    void testRejectsNonEnclaveWithoutPhysicalAd() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .region("us-phoenix-1")
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(failingDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }

    @Test
    void testRejectsNonEnclaveWithoutRegionOrDefault() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .physicalAd("AD-1")
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig),
                ociEnvLocationDefaults(emptyDefaultRegion()));
        assertThrows(IllegalStateException.class, factory::get);
    }
}
