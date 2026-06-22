/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

/**
 * Provides service-principal-specific configuration without extending the common OCI config contract.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class ServicePrincipalConfigProvider implements Supplier<ServicePrincipalMethodConfig> {
    private static final String CONFIG_PREFIX = "helidon.oci.authentication.service-principal";

    private final Config config;

    @Service.Inject
    ServicePrincipalConfigProvider(Config config) {
        this.config = config;
    }

    static ServicePrincipalConfigProvider create(Config config) {
        return new ServicePrincipalConfigProvider(config);
    }

    @Override
    public ServicePrincipalMethodConfig get() {
        Config servicePrincipalConfig = config.get(CONFIG_PREFIX);
        if (servicePrincipalConfig.exists()) {
            return ServicePrincipalMethodConfig.create(servicePrincipalConfig);
        }
        return ServicePrincipalMethodConfig.create();
    }
}
