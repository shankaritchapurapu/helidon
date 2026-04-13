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
        AuthorizationClientFactory factory = new AuthorizationClientFactory(identityConfigFactory());
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
                identityConfigFactory(authenticationConfig(false), authorizationConfig));
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
    }

    @Test
    void testRejectsEnclaveWithoutAd() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .serviceEnclave(true)
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig));
        assertThrows(IllegalStateException.class, factory::get);
    }

    @Test
    void testRejectsNonEnclaveWithoutPhysicalAd() {
        AuthorizationConfig authorizationConfig = AuthorizationConfig.builder()
                .serviceName("service")
                .region("us-phoenix-1")
                .build();

        AuthorizationClientFactory factory = new AuthorizationClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig));
        assertThrows(IllegalStateException.class, factory::get);
    }
}
