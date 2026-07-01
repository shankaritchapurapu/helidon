/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.lang.System.Logger.Level;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;

/**
 * Provides service-principal-specific configuration from the raw configuration retained by the common OCI config
 * contract.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class ServicePrincipalConfigProvider implements Supplier<ServicePrincipalMethodConfig> {
    private static final System.Logger LOGGER = System.getLogger(ServicePrincipalConfigProvider.class.getName());
    private static final String CONFIG_KEY = "authentication.service-principal";

    private final OciConfig config;

    @Service.Inject
    ServicePrincipalConfigProvider(OciConfig config) {
        this.config = config;
    }

    static ServicePrincipalConfigProvider create(OciConfig config) {
        return new ServicePrincipalConfigProvider(config);
    }

    @Override
    public ServicePrincipalMethodConfig get() {
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG, "Reading service-principal configuration from OciConfig");
        }
        return config.config()
                .map(ociConfig -> ociConfig.get(CONFIG_KEY))
                .filter(io.helidon.common.config.Config::exists)
                .map(ServicePrincipalMethodConfig::create)
                .orElseGet(ServicePrincipalMethodConfig::create);
    }
}
