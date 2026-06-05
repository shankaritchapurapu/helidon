/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.Service;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;

/**
 * Session token authentication method, uses the {@link com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider}.
 */
@Weight(Weighted.DEFAULT_WEIGHT - 15)
@Service.Provider
class AuthenticationMethodSessionToken implements OciAuthenticationMethod {
    static final String DEFAULT_PROFILE_NAME = "DEFAULT";
    static final String METHOD = "session-token";

    private static final System.Logger LOGGER = System.getLogger(AuthenticationMethodSessionToken.class.getName());

    private final LazyValue<Optional<BasicAuthenticationDetailsProvider>> provider;

    AuthenticationMethodSessionToken(OciConfig config,
                                     Supplier<Optional<ConfigFileReader.ConfigFile>> configFileSupplier,
                                     Supplier<Optional<SessionTokenAuthenticationDetailsProvider>> providerSupplier) {
        provider = LazyValue.create(() -> createProvider(config, configFileSupplier, providerSupplier));
    }

    @Override
    public String method() {
        return METHOD;
    }

    @Override
    public Optional<BasicAuthenticationDetailsProvider> provider() {
        return provider.get();
    }

    private static Optional<BasicAuthenticationDetailsProvider>
    createProvider(OciConfig config,
                   Supplier<Optional<ConfigFileReader.ConfigFile>> configFileSupplier,
                   Supplier<Optional<SessionTokenAuthenticationDetailsProvider>> providerSupplier) {

        /*
        Explicit authentication.session-token configuration takes precedence and intentionally short-circuits OCI config file lookup. Config file lookup is only for implicit selection through security_token_file.
         */

        Optional<SessionTokenMethodConfig> maybeSessionTokenConfig = config.sessionTokenMethodConfig();

        if (maybeSessionTokenConfig.isPresent() || hasSecurityToken(configFileSupplier.get())) {
            try {
                return providerSupplier.get()
                        .map(BasicAuthenticationDetailsProvider.class::cast);
            } catch (UncheckedIOException e) {
                if (LOGGER.isLoggable(Level.TRACE)) {
                    LOGGER.log(Level.TRACE, "Cannot create session token authentication provider", e);
                }
                return Optional.empty();
            }
        }

        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Session token authentication provider is not configured");
        }
        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static boolean hasSecurityToken(Optional<ConfigFileReader.ConfigFile> maybeConfigFile) {
        if (maybeConfigFile.isPresent()) {
            ConfigFileReader.ConfigFile configFile = maybeConfigFile.get();
            return configFile.get("security_token_file") != null;
        }
        return false;
    }
}
