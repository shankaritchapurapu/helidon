/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.helidon.common.Api;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Qualifier;
import io.helidon.service.registry.Service;

import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;

/**
 * Factory that exposes configured dynamic SSL context providers as named services.
 */
@Service.Singleton
@Service.Named(Service.Named.WILDCARD_NAME)
@Api.Internal
@Weight(Weighted.DEFAULT_WEIGHT - 30)
public final class DynamicSslContextProviderConfigFactory
        implements Service.ServicesFactory<DynamicSslContextProviderConfig> {
    private final List<Service.QualifiedInstance<DynamicSslContextProviderConfig>> services;

    /**
     * Create a new factory.
     *
     * @param config generated provider configuration
     */
    @Service.Inject
    public DynamicSslContextProviderConfigFactory(DynamicSslProvidersConfig config) {
        this.services = providerServices(config.dynamicSslContextProviders());
    }

    @Override
    public List<Service.QualifiedInstance<DynamicSslContextProviderConfig>> services() {
        return services;
    }

    /**
     * Convert declarative configuration to the PIC dynamic SSL context provider configuration.
     *
     * @param config declarative configuration
     * @return PIC dynamic SSL context provider configuration
     */
    public static DynamicSslContextProviderConfig toProviderConfig(DynamicSslProviderConfig config) {
        DynamicSslContextProviderConfig providerConfig = new DynamicSslContextProviderConfig();
        config.leafCertPath().ifPresent(providerConfig::setLeafCertPath);
        config.leafCertKeyPath().ifPresent(providerConfig::setLeafCertKeyPath);
        config.leafCertKeyPassphrase().ifPresent(providerConfig::setLeafCertKeyPassphrase);
        config.intermediateCertPath().ifPresent(providerConfig::setIntermediateCertPath);
        providerConfig.setRootCertPath(config.rootCertPath());
        config.duration().ifPresent(providerConfig::setDuration);
        config.sslAlgorithm().ifPresent(providerConfig::setSslAlgorithm);
        return providerConfig;
    }

    private static List<Service.QualifiedInstance<DynamicSslContextProviderConfig>> providerServices(
            Iterable<DynamicSslProviderConfig> configs) {
        Set<String> names = new LinkedHashSet<>();
        List<Service.QualifiedInstance<DynamicSslContextProviderConfig>> providers = new ArrayList<>();
        for (DynamicSslProviderConfig config : configs) {
            String name = config.name();
            if (!names.add(name)) {
                throw new IllegalStateException("Duplicate dynamic SSL context provider configured with name '"
                                                        + name + "'");
            }
            providers.add(Service.QualifiedInstance.create(
                    toProviderConfig(config),
                    Qualifier.createNamed(name)));
        }
        return List.copyOf(providers);
    }
}
