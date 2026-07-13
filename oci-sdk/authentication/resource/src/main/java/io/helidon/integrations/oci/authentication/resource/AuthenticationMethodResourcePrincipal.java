/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.resource;

import java.lang.System.Logger.Level;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.OciResourcePrincipalProvider;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder;

/**
 * Resource principal authentication method, uses the
 * {@link com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider}.
 */
@Weight(Weighted.DEFAULT_WEIGHT - 30)
@Service.Provider
class AuthenticationMethodResourcePrincipal implements OciAuthenticationMethod, OciResourcePrincipalProvider {
    private static final System.Logger LOGGER = System.getLogger(AuthenticationMethodResourcePrincipal.class.getName());
    private static final String RESOURCE_PRINCIPAL_VERSION_ENV_VAR = "OCI_RESOURCE_PRINCIPAL_VERSION";
    private static final String METHOD = "resource-principal";

    private final LazyValue<Optional<BasicAuthenticationDetailsProvider>> provider;

    AuthenticationMethodResourcePrincipal(Supplier<Optional<ResourcePrincipalAuthenticationDetailsProviderBuilder>> builder) {
        provider = createProvider(builder);
    }

    @Override
    public String method() {
        return METHOD;
    }

    @Override
    public Optional<BasicAuthenticationDetailsProvider> provider() {
        return provider.get();
    }

    private static LazyValue<Optional<BasicAuthenticationDetailsProvider>>
    createProvider(Supplier<Optional<ResourcePrincipalAuthenticationDetailsProviderBuilder>> builder) {
        return LazyValue.create(() -> {
            // The OCI SDK requires this environment variable before it can build a resource principal provider.
            if (System.getenv(RESOURCE_PRINCIPAL_VERSION_ENV_VAR) == null) {
                if (LOGGER.isLoggable(Level.TRACE)) {
                    LOGGER.log(Level.TRACE, "Environment variable \"" + RESOURCE_PRINCIPAL_VERSION_ENV_VAR
                            + "\" is not set, resource principal cannot be used.");
                }
                return Optional.empty();
            }
            return builder.get()
                    .map(ResourcePrincipalAuthenticationDetailsProviderBuilder::build);
        });
    }
}
