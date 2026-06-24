/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.lang.System.Logger.Level;
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
            Optional<ServicePrincipalMethodConfig> maybeServicePrincipalConfig = servicePrincipalConfig.get();
            boolean useInstancePrincipal =
                    ServicePrincipalBuilderProvider.useInstancePrincipal(maybeServicePrincipalConfig);
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Resolving service-principal authentication provider; "
                                   + "servicePrincipalConfigPresent={0}, useInstancePrincipal={1}",
                           maybeServicePrincipalConfig.isPresent(),
                           useInstancePrincipal);
            }
            if (useInstancePrincipal) {
                if (!HelidonOci.imdsAvailable(config)) {
                    if (LOGGER.isLoggable(Level.DEBUG)) {
                        LOGGER.log(Level.DEBUG, "OCI Metadata service is not available; "
                                + "service-principal authentication provider cannot be created from "
                                + "instance-principal certificate material");
                    }
                    return Optional.empty();
                }
            }
            Optional<S2SAuthenticationDetailsProviderBuilder> maybeBuilder = builder.get();
            if (maybeBuilder.isEmpty()) {
                if (LOGGER.isLoggable(Level.DEBUG)) {
                    LOGGER.log(Level.DEBUG, "Service-principal authentication provider builder is not available");
                }
                return Optional.empty();
            }
            BasicAuthenticationDetailsProvider provider = maybeBuilder.get().build();
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Created service-principal authentication provider; providerType={0}",
                           provider.getClass().getName());
            }
            return Optional.of(provider);
        });
    }
}
