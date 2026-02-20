/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.bmc.ConfigFileReader.ConfigFile;

@Service.Provider
class AdpSessionTokenBuilderProvider implements Supplier<SessionTokenAuthenticationDetailsProviderBuilder> {

    private final Supplier<Optional<ConfigFile>> configFileSupplier;

    AdpSessionTokenBuilderProvider(Supplier<Optional<ConfigFile>> configFileSupplier) {
        this.configFileSupplier = configFileSupplier;
    }

    @Override
    public SessionTokenAuthenticationDetailsProviderBuilder get() {
        return new SessionTokenAuthenticationDetailsProviderBuilder(configFileSupplier.get().orElseThrow());
    }

    Optional<String> value(ConfigFile file, String key) {
        return Optional.ofNullable(file.get(key));
    }

}
