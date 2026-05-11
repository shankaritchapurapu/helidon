/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.tagging;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.service.registry.Service;

import com.oracle.bmc.identity.Identity;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;

import static org.mockito.Mockito.mock;

final class TaggingEndpointTestServices {
    static final AuthenticatorClient AUTHENTICATOR = mock(AuthenticatorClient.class);
    static final IAuthorizationClient AUTHORIZATION = mock(IAuthorizationClient.class);
    static final Identity IDENTITY = mock(Identity.class);

    private TaggingEndpointTestServices() {
    }
}

@Weight(1000.0)
@Service.Singleton
class TestAuthenticatorClientSupplier implements Supplier<AuthenticatorClient> {
    @Override
    public AuthenticatorClient get() {
        return TaggingEndpointTestServices.AUTHENTICATOR;
    }
}

@Weight(1000.0)
@Service.Singleton
class TestAuthorizationClientSupplier implements Supplier<IAuthorizationClient> {
    @Override
    public IAuthorizationClient get() {
        return TaggingEndpointTestServices.AUTHORIZATION;
    }
}

@Weight(1000.0)
@Service.Singleton
class TestIdentitySupplier implements Supplier<Identity> {
    @Override
    public Identity get() {
        return TaggingEndpointTestServices.IDENTITY;
    }
}
