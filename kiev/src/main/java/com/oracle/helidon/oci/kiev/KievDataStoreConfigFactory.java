/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.pic.commons.s2s.util.OfflineAuth;
import com.oracle.pic.kiev.DataStoreConfig;
import com.oracle.pic.kiev.DirectDbStoreConfig;
import com.oracle.pic.kiev.KaasStoreConfig;
import com.oracle.pic.kiev.auth.AuthDetailsConfig;
import com.oracle.pic.kiev.mapping.InMemoryDataStoreConfig;
import com.oracle.pic.kiev.registry.config.ClientRegistryConfig;

/**
 * Factory for creating Kiev {@link DataStoreConfig} instances from Helidon configuration.
 */
@Service.Singleton
class KievDataStoreConfigFactory implements Supplier<DataStoreConfig> {

    private final KievConfig kievConfig;

    @Service.Inject
    KievDataStoreConfigFactory(KievConfig kievConfig) {
        this.kievConfig = kievConfig;
    }

    @Override
    public DataStoreConfig get() {
        DataStoreConfig dataStoreConfig = switch (kievConfig.backend()) {
        case IN_MEMORY -> new InMemoryDataStoreConfig(kievConfig.storeName(), kievConfig.appName());
        case DIRECT_DB -> createDirectDbConfig();
        case SERVICE -> createServiceConfig();
        };
        dataStoreConfig.setTransactionMaxReads(kievConfig.transactionMaxReads());
        dataStoreConfig.setTransactionMaxWrites(kievConfig.transactionMaxWrites());
        return dataStoreConfig;
    }

    private DirectDbStoreConfig createDirectDbConfig() {
        KievDirectDbConfig directDbConfig = kievConfig.directDb()
                .orElseThrow(() -> new IllegalStateException("oci.kiev.direct-db must be configured for DIRECT_DB backend"));

        DirectDbStoreConfig dataStoreConfig = new DirectDbStoreConfig(kievConfig.storeName(),
                                                                      kievConfig.appName(),
                                                                      directDbConfig.jdbcUrl(),
                                                                      directDbConfig.userName(),
                                                                      directDbConfig.password());
        directDbConfig.schemaName().ifPresent(dataStoreConfig::setSchemaName);
        return dataStoreConfig;
    }

    private KaasStoreConfig createServiceConfig() {
        KievServiceConfig serviceConfig = kievConfig.service()
                .orElseThrow(() -> new IllegalStateException("oci.kiev.service must be configured for SERVICE backend"));
        KievServiceAuthConfig authConfig = serviceConfig.auth()
                .orElseThrow(() -> new IllegalStateException("oci.kiev.service.auth must be configured for SERVICE backend"));

        KaasStoreConfig dataStoreConfig = new KaasStoreConfig(kievConfig.storeName(), kievConfig.appName());
        dataStoreConfig.setCompartmentId(serviceConfig.compartmentId());
        dataStoreConfig.setFrontendEndpoint(serviceConfig.frontendEndpoint());
        dataStoreConfig.setLocality(serviceConfig.locality());
        dataStoreConfig.setAuthDetailsConfig(toAuthDetailsConfig(authConfig));
        if (authConfig.type() == KievAuthType.KIAB_LOCAL) {
            dataStoreConfig.setRegistryConfig(ClientRegistryConfig.builder()
                                                    .enabled(false)
                                                    .endpointOverride(serviceConfig.frontendEndpoint())
                                                    .build());
        }
        return dataStoreConfig;
    }

    private AuthDetailsConfig toAuthDetailsConfig(KievServiceAuthConfig authConfig) {
        return switch (authConfig.type()) {
        case INSTANCE -> toInstanceAuthConfig(authConfig);
        case S2S -> toS2sAuthConfig(authConfig);
        case KIAB_LOCAL -> toKiabLocalAuthConfig();
        };
    }

    private AuthDetailsConfig.InstanceAuthDetailsConfig toInstanceAuthConfig(KievServiceAuthConfig authConfig) {
        AuthDetailsConfig.InstanceAuthDetailsConfig config = new AuthDetailsConfig.InstanceAuthDetailsConfig();
        authConfig.authEndpoint().ifPresent(config::setAuthEndpoint);
        config.setRootCertPemPath(requiredValue(authConfig.rootCertPemPath(), "oci.kiev.service.auth.root-cert-pem-path"));
        authConfig.certReloadDuration().ifPresent(config::setCertReloadDuration);
        authConfig.certSslAlgorithm().ifPresent(config::setCertSslAlgorithm);
        return config;
    }

    private AuthDetailsConfig.S2sAuthDetailsConfig toS2sAuthConfig(KievServiceAuthConfig authConfig) {
        AuthDetailsConfig.S2sAuthDetailsConfig config = new AuthDetailsConfig.S2sAuthDetailsConfig();
        config.setAuthEndpoint(requiredValue(authConfig.authEndpoint(), "oci.kiev.service.auth.auth-endpoint"));
        config.setRootCertPemPath(requiredValue(authConfig.rootCertPemPath(), "oci.kiev.service.auth.root-cert-pem-path"));
        config.setLeafCertPath(requiredValue(authConfig.leafCertPath(), "oci.kiev.service.auth.leaf-cert-path"));
        config.setLeafCertKeyPath(requiredValue(authConfig.leafCertKeyPath(), "oci.kiev.service.auth.leaf-cert-key-path"));
        config.setIntermediateCertPath(requiredValue(authConfig.intermediateCertPath(),
                                                     "oci.kiev.service.auth.intermediate-cert-path"));
        config.setTenantId(requiredValue(authConfig.tenantId(), "oci.kiev.service.auth.tenant-id"));
        authConfig.keyPassphrase().ifPresent(config::setKeyPassphrase);
        authConfig.certReloadDuration().ifPresent(config::setCertReloadDuration);
        authConfig.certSslAlgorithm().ifPresent(config::setCertSslAlgorithm);
        return config;
    }

    private AuthDetailsConfig.OverriddenAuthDetailsConfig toKiabLocalAuthConfig() {
        AuthDetailsConfig.OverriddenAuthDetailsConfig config = new AuthDetailsConfig.OverriddenAuthDetailsConfig();
        config.setAuthProviderOverride(OfflineAuth.authProvider());
        return config;
    }

    private static <T> T requiredValue(java.util.Optional<T> value, String key) {
        return value.orElseThrow(() -> new IllegalStateException(key + " must be configured"));
    }
}
