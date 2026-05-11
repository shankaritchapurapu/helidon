/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.identity.client;

import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Factory for {@link IdentityClientConfig}.
 */
@Service.Singleton
class IdentityClientConfigFactory implements Supplier<IdentityClientConfig> {

    static final String OCI_IDENTITY_CLIENT = "oci.identity-client";
    private final Config config;

    @Service.Inject
    IdentityClientConfigFactory(Config config) {
        this.config = config;
    }

    @Override
    public IdentityClientConfig get() {
        return IdentityClientConfig.create(config.get(OCI_IDENTITY_CLIENT));
    }
}
