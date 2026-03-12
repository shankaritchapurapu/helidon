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

class AuthorizationClientFactoryTest extends BaseAuthenticationClientTest {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        if (INITIALIZED.compareAndSet(false, true)) {
            ServiceAuthenticationClient serviceAuthClient = serviceAuthenticationClient();
            assertThat(serviceAuthClient, notNullValue());
            Services.set(ServiceAuthenticationClient.class, serviceAuthClient);
        }
    }

    @Test
    void testClientCreation() {
        AuthorizationClientFactory factory = new AuthorizationClientFactory(identityConfigFactory());
        Optional<IAuthorizationClient> client = factory.get();
        assertThat(client.isPresent(), is(true));
        assertThat(client.get(), is(notNullValue()));
    }
}
