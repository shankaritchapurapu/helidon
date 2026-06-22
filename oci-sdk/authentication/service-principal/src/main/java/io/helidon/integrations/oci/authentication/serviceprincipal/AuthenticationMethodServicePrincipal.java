/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.HelidonOci;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider.S2SAuthenticationDetailsProviderBuilder;

/**
 * Service principal authentication method, uses the
 * {@link com.oracle.bmc.auth.S2SAuthenticationDetailsProvider}.
 */
@Weight(Weighted.DEFAULT_WEIGHT - 45)
@Service.Provider
class AuthenticationMethodServicePrincipal implements OciAuthenticationMethod {
    static final String METHOD = "service-principal";

    private static final System.Logger LOGGER =
            System.getLogger(AuthenticationMethodServicePrincipal.class.getName());

    private final LazyValue<Optional<BasicAuthenticationDetailsProvider>> provider;

    AuthenticationMethodServicePrincipal(OciConfig config,
                                         Supplier<Optional<ServicePrincipalMethodConfig>> servicePrincipalConfig,
                                         Supplier<Optional<S2SAuthenticationDetailsProviderBuilder>> builder) {
        provider = createProvider(config, servicePrincipalConfig, builder);
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
            createProvider(OciConfig config,
                           Supplier<Optional<ServicePrincipalMethodConfig>> servicePrincipalConfig,
                           Supplier<Optional<S2SAuthenticationDetailsProviderBuilder>> builder) {
        return LazyValue.create(() -> {
            if (ServicePrincipalBuilderProvider.useInstancePrincipal(servicePrincipalConfig)) {
                if (!HelidonOci.imdsAvailable(config)) {
                    if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                        LOGGER.log(System.Logger.Level.TRACE, "OCI Metadata service is not available, "
                                + "service principal cannot be used.");
                    }
                    return Optional.empty();
                }
            }
            return builder.get()
                    .map(S2SAuthenticationDetailsProviderBuilder::build);
        });
    }
}
