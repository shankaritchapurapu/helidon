/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.HelidonOci;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.integrations.oci.OciResourcePrincipalProvider;
import io.helidon.integrations.oci.OciServicePrincipalProvider;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.Service;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RpS2SAuthenticationDetailsProvider.RpS2SAuthenticationDetailsProviderBuilder;
import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider.S2SAuthenticationDetailsProviderBuilder;

/**
 * Service principal authentication method. It uses
 * {@link com.oracle.bmc.auth.S2SAuthenticationDetailsProvider} with instance-principal credentials
 * and {@link com.oracle.bmc.auth.RpS2SAuthenticationDetailsProvider} with resource-principal
 * credentials.
 */
@Weight(Weighted.DEFAULT_WEIGHT - 45)
@Service.Provider
class AuthenticationMethodServicePrincipal implements OciAuthenticationMethod, OciServicePrincipalProvider {
    static final String METHOD = "service-principal";

    private static final System.Logger LOGGER =
            System.getLogger(AuthenticationMethodServicePrincipal.class.getName());

    private final LazyValue<Optional<BasicAuthenticationDetailsProvider>> provider;

    AuthenticationMethodServicePrincipal(OciConfig config,
                                         Supplier<Optional<ServicePrincipalMethodConfig>> servicePrincipalConfig,
                                         Supplier<Optional<S2SAuthenticationDetailsProviderBuilder>> builder,
                                         List<OciResourcePrincipalProvider> resourcePrincipalProviders) {
        provider = createProvider(config, servicePrincipalConfig, builder, resourcePrincipalProviders);
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
                           Supplier<Optional<S2SAuthenticationDetailsProviderBuilder>> builder,
                           List<OciResourcePrincipalProvider> resourcePrincipalProviders) {
        return LazyValue.create(() -> {
            for (OciResourcePrincipalProvider resourcePrincipalProvider : resourcePrincipalProviders) {
                Optional<BasicAuthenticationDetailsProvider> resourcePrincipal = resourcePrincipalProvider.provider();
                if (resourcePrincipal.isPresent()) {
                    if (LOGGER.isLoggable(Level.DEBUG)) {
                        LOGGER.log(Level.DEBUG,
                                   "Creating service-principal authentication provider from resource-principal "
                                           + "credentials; providerType={0}",
                                   resourcePrincipal.get().getClass().getName());
                    }
                    return Optional.of(createResourceServicePrincipal(config, resourcePrincipal.get()));
                }
            }

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

    private static BasicAuthenticationDetailsProvider createResourceServicePrincipal(
            OciConfig config,
            BasicAuthenticationDetailsProvider resourcePrincipal) {
        ServicePrincipalRpS2SAuthenticationDetailsProviderBuilder builder =
                new ServicePrincipalRpS2SAuthenticationDetailsProviderBuilder();
        config.region().ifPresent(builder::region);
        builder.provider(resourcePrincipal)
                .federationEndpoint(config.federationEndpoint()
                                            .map(URI::toString)
                                            .orElseThrow(() -> new IllegalStateException(
                                                    "helidon.oci.federation-endpoint must be configured to exchange "
                                                            + "a resource principal for a service principal")))
                .tenancyId(config.tenantId()
                                   .orElseThrow(() -> new IllegalStateException(
                                           "helidon.oci.tenant-id must be configured to exchange a resource "
                                                   + "principal for a service principal")));
        return builder.build();
    }

    private static final class ServicePrincipalRpS2SAuthenticationDetailsProviderBuilder
            extends RpS2SAuthenticationDetailsProviderBuilder {

        private ServicePrincipalRpS2SAuthenticationDetailsProviderBuilder region(Region region) {
            this.region = region;
            return this;
        }
    }
}
