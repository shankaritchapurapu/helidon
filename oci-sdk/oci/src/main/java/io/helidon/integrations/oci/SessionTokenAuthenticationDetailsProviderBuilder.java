/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.io.IOException;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;

/**
 * Builder for {@link SessionTokenAuthenticationDetailsProvider}.
 */
class SessionTokenAuthenticationDetailsProviderBuilder {

    private final ConfigFileReader.ConfigFile config;

    SessionTokenAuthenticationDetailsProviderBuilder(ConfigFileReader.ConfigFile config) {
        this.config = config;
    }

    SessionTokenAuthenticationDetailsProvider build() throws IOException {
        return new SessionTokenAuthenticationDetailsProvider(config);
    }
}
