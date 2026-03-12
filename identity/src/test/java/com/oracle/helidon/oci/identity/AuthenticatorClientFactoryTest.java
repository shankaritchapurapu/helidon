/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.service.registry.Services;

import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

class AuthenticatorClientFactoryTest extends BaseAuthenticationClientTest {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        if (INITIALIZED.compareAndSet(false, true)) {
            ServiceAuthenticationClient serviceAuthClient = serviceAuthenticationClient();
            assertThat(serviceAuthClient, notNullValue());
            Services.set(ServiceAuthenticationClient.class, serviceAuthClient);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testClientCreation(boolean hardCodedKeys) {
        AuthenticatorClientFactory factory = new AuthenticatorClientFactory(identityConfigFactory(hardCodedKeys));
        AuthenticatorClient client = factory.get();
        assertThat(client, notNullValue());
    }
}
