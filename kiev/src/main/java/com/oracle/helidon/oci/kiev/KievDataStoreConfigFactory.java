/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.commons.s2s.util.OfflineAuth;
import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
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
    private final Optional<BasicAuthenticationDetailsProvider> authProvider;

    @Service.Inject
    KievDataStoreConfigFactory(KievConfig kievConfig,
                               Optional<BasicAuthenticationDetailsProvider> authProvider) {
        this.kievConfig = kievConfig;
        this.authProvider = authProvider;
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
        case OVERRIDDEN -> toOverriddenAuthConfig();
        case KIAB_LOCAL -> toKiabLocalAuthConfig();
        };
    }

    private AuthDetailsConfig.InstanceAuthDetailsConfig toInstanceAuthConfig(KievServiceAuthConfig authConfig) {
        KievServiceTlsConfig tlsConfig = authConfig.tls()
                .orElseThrow(() -> new IllegalStateException("oci.kiev.service.auth.tls must be configured"));
        AuthDetailsConfig.InstanceAuthDetailsConfig config = new AuthDetailsConfig.InstanceAuthDetailsConfig();
        authConfig.authEndpoint().ifPresent(config::setAuthEndpoint);
        config.setRootCertPemPath(requiredValue(tlsConfig.rootCertPemPath(), "oci.kiev.service.auth.tls.root-cert-pem-path"));
        tlsConfig.certReloadDuration().ifPresent(config::setCertReloadDuration);
        tlsConfig.certSslAlgorithm().ifPresent(config::setCertSslAlgorithm);
        return config;
    }

    private AuthDetailsConfig.S2sAuthDetailsConfig toS2sAuthConfig(KievServiceAuthConfig authConfig) {
        KievServiceTlsConfig tlsConfig = authConfig.tls()
                .orElseThrow(() -> new IllegalStateException("oci.kiev.service.auth.tls must be configured"));
        KievServiceS2sConfig s2sConfig = authConfig.s2s()
                .orElseThrow(() -> new IllegalStateException("oci.kiev.service.auth.s2s must be configured"));
        AuthDetailsConfig.S2sAuthDetailsConfig config = new AuthDetailsConfig.S2sAuthDetailsConfig();
        config.setAuthEndpoint(requiredValue(authConfig.authEndpoint(), "oci.kiev.service.auth.auth-endpoint"));
        config.setRootCertPemPath(requiredValue(tlsConfig.rootCertPemPath(), "oci.kiev.service.auth.tls.root-cert-pem-path"));
        config.setLeafCertPath(requiredValue(s2sConfig.leafCertPath(), "oci.kiev.service.auth.s2s.leaf-cert-path"));
        config.setLeafCertKeyPath(requiredValue(s2sConfig.leafCertKeyPath(), "oci.kiev.service.auth.s2s.leaf-cert-key-path"));
        config.setIntermediateCertPath(requiredValue(s2sConfig.intermediateCertPath(),
                                                     "oci.kiev.service.auth.s2s.intermediate-cert-path"));
        config.setTenantId(requiredValue(s2sConfig.tenantId(), "oci.kiev.service.auth.s2s.tenant-id"));
        s2sConfig.keyPassphrase().ifPresent(config::setKeyPassphrase);
        tlsConfig.certReloadDuration().ifPresent(config::setCertReloadDuration);
        tlsConfig.certSslAlgorithm().ifPresent(config::setCertSslAlgorithm);
        return config;
    }

    private AuthDetailsConfig.OverriddenAuthDetailsConfig toKiabLocalAuthConfig() {
        return toOverriddenAuthConfig(OfflineAuth.authProvider());
    }

    private AuthDetailsConfig.OverriddenAuthDetailsConfig toOverriddenAuthConfig() {
        return toOverriddenAuthConfig(authProvider.orElseThrow(() -> new IllegalStateException(
                "oci.kiev.service.auth.type=OVERRIDDEN requires BasicAuthenticationDetailsProvider to be available")),
                                      kievConfig.service()
                                              .flatMap(KievServiceConfig::auth)
                                              .flatMap(KievServiceAuthConfig::tls));
    }

    private static AuthDetailsConfig.OverriddenAuthDetailsConfig toOverriddenAuthConfig(
            BasicAuthenticationDetailsProvider authProvider) {
        return toOverriddenAuthConfig(authProvider, Optional.empty());
    }

    private static AuthDetailsConfig.OverriddenAuthDetailsConfig toOverriddenAuthConfig(
            BasicAuthenticationDetailsProvider authProvider,
            Optional<KievServiceTlsConfig> tlsConfig) {
        AuthDetailsConfig.OverriddenAuthDetailsConfig config = new AuthDetailsConfig.OverriddenAuthDetailsConfig();
        config.setAuthProviderOverride(authProvider);
        tlsConfig.map(KievDataStoreConfigFactory::toDynamicSslContextProviderConfig)
                .ifPresent(config::setDynamicSslContextProviderConfig);
        return config;
    }

    private static DynamicSslContextProviderConfig toDynamicSslContextProviderConfig(KievServiceTlsConfig tlsConfig) {
        DynamicSslContextProviderConfig config = new DynamicSslContextProviderConfig();
        config.setRootCertPath(requiredValue(tlsConfig.rootCertPemPath(), "oci.kiev.service.auth.tls.root-cert-pem-path"));
        tlsConfig.certReloadDuration().ifPresent(config::setDuration);
        tlsConfig.certSslAlgorithm().ifPresent(config::setSslAlgorithm);
        return config;
    }

    private static <T> T requiredValue(java.util.Optional<T> value, String key) {
        return value.orElseThrow(() -> new IllegalStateException(key + " must be configured"));
    }
}
