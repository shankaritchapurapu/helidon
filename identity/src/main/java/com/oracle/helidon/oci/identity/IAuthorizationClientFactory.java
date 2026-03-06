/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;

/**
 * A factory class responsible for creating instances of {@link IAuthorizationClient}.
 */
@Service.Singleton
public class IAuthorizationClientFactory implements Supplier<Optional<IAuthorizationClient>> {

    private final AuthorizationConfig config;

    IAuthorizationClientFactory(IdentityConfigFactory config) {
        this.config = config.get().authorization();
    }

    @Override
    public Optional<IAuthorizationClient> get() {
        return Optional.empty();
    }
}
