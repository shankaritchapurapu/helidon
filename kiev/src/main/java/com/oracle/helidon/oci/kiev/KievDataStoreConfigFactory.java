/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.service.registry.ServiceRegistry;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.commons.s2s.util.OfflineAuth;
import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import com.oracle.pic.kiev.DataStoreConfig;
import com.oracle.pic.kiev.DirectDbStoreConfig;
import com.oracle.pic.kiev.KaasStoreConfig;
import com.oracle.pic.kiev.auth.AuthDetailsConfig;
import com.oracle.pic.kiev.mapping.InMemoryDataStoreConfig;
import com.oracle.pic.kiev.registry.config.ClientRegistryConfig;
import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;

/**
 * Factory for creating Kiev {@link DataStoreConfig} instances from Helidon configuration.
 */
class KievDataStoreConfigFactory {
    private static final String DATA_STORES_CONFIG_KEY = "oci.kiev.data-stores[]";

    private KievDataStoreConfigFactory() {
    }

    static DataStoreConfig create(KievStoreConfig storeConfig,
                                  Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
                                  ServiceRegistry serviceRegistry) {
        return create(new StoreDefinition(storeConfig.backend(),
                                          storeConfig.storeName(),
                                          storeConfig.appName(),
                                          storeConfig.transactionMaxReads(),
                                          storeConfig.transactionMaxWrites(),
                                          storeConfig.directDb(),
                                          storeConfig.service(),
                                          authProvider,
                                          serviceRegistry));
    }

    private static DataStoreConfig create(StoreDefinition store) {
        validateRequired(store.storeName(), dataStoreKey("store-name"));
        validateRequired(store.appName(), dataStoreKey("app-name"), store.storeName());

        DataStoreConfig dataStoreConfig = switch (store.backend()) {
        case IN_MEMORY -> new InMemoryDataStoreConfig(store.storeName(), store.appName());
        case DIRECT_DB -> createDirectDbConfig(store);
        case SERVICE -> createServiceConfig(store);
        };
        dataStoreConfig.setTransactionMaxReads(store.transactionMaxReads());
        dataStoreConfig.setTransactionMaxWrites(store.transactionMaxWrites());
        return dataStoreConfig;
    }

    private static DirectDbStoreConfig createDirectDbConfig(StoreDefinition store) {
        KievDirectDbConfig directDbConfig = store.directDb()
                .orElseThrow(() -> new IllegalStateException(missingBackendConfigMessage(dataStoreKey("direct-db"),
                                                                                         KievBackend.DIRECT_DB,
                                                                                         store.storeName())));

        DirectDbStoreConfig dataStoreConfig = new DirectDbStoreConfig(store.storeName(),
                                                                      store.appName(),
                                                                      directDbConfig.jdbcUrl(),
                                                                      directDbConfig.userName(),
                                                                      directDbConfig.password());
        directDbConfig.schemaName().ifPresent(dataStoreConfig::setSchemaName);
        return dataStoreConfig;
    }

    private static KaasStoreConfig createServiceConfig(StoreDefinition store) {
        KievServiceConfig serviceConfig = store.service()
                .orElseThrow(() -> new IllegalStateException(missingBackendConfigMessage(dataStoreKey("service"),
                                                                                         KievBackend.SERVICE,
                                                                                         store.storeName())));
        KievServiceAuthConfig authConfig = serviceConfig.auth()
                .orElseThrow(() -> new IllegalStateException(missingBackendConfigMessage(dataStoreKey("service.auth"),
                                                                                         KievBackend.SERVICE,
                                                                                         store.storeName())));

        KaasStoreConfig dataStoreConfig = new KaasStoreConfig(store.storeName(), store.appName());
        dataStoreConfig.setCompartmentId(serviceConfig.compartmentId());
        dataStoreConfig.setFrontendEndpoint(serviceConfig.frontendEndpoint());
        dataStoreConfig.setLocality(serviceConfig.locality().orElse(ClientRegistryLocality.REGIONAL));
        dataStoreConfig.setAuthDetailsConfig(toAuthDetailsConfig(authConfig, store));
        if (authConfig.type() == KievAuthType.KIAB_LOCAL) {
            dataStoreConfig.setRegistryConfig(ClientRegistryConfig.builder()
                                                    .enabled(false)
                                                    .endpointOverride(serviceConfig.frontendEndpoint())
                                                    .build());
        }
        return dataStoreConfig;
    }

    private static AuthDetailsConfig toAuthDetailsConfig(KievServiceAuthConfig authConfig, StoreDefinition store) {
        return switch (authConfig.type()) {
        case INSTANCE -> toInstanceAuthConfig(authConfig, store.storeName());
        case S2S -> toS2sAuthConfig(authConfig, store.storeName());
        case OVERRIDDEN -> toOverriddenAuthConfig(authConfig, store);
        case KIAB_LOCAL -> toKiabLocalAuthConfig();
        };
    }

    private static AuthDetailsConfig.InstanceAuthDetailsConfig toInstanceAuthConfig(KievServiceAuthConfig authConfig,
                                                                                   String storeName) {
        KievServiceTlsConfig tlsConfig = requiredValue(authConfig.tls(),
                                                       dataStoreKey("service.auth.tls"),
                                                       storeName);
        AuthDetailsConfig.InstanceAuthDetailsConfig config = new AuthDetailsConfig.InstanceAuthDetailsConfig();
        authConfig.authEndpoint().ifPresent(config::setAuthEndpoint);
        config.setRootCertPemPath(requiredValue(tlsConfig.rootCertPemPath(),
                                                dataStoreKey("service.auth.tls.root-cert-pem-path"),
                                                storeName));
        tlsConfig.certReloadDuration().ifPresent(config::setCertReloadDuration);
        tlsConfig.certSslAlgorithm().ifPresent(config::setCertSslAlgorithm);
        return config;
    }

    private static AuthDetailsConfig.S2sAuthDetailsConfig toS2sAuthConfig(KievServiceAuthConfig authConfig,
                                                                          String storeName) {
        KievServiceTlsConfig tlsConfig = requiredValue(authConfig.tls(),
                                                       dataStoreKey("service.auth.tls"),
                                                       storeName);
        KievServiceS2sConfig s2sConfig = requiredValue(authConfig.s2s(),
                                                       dataStoreKey("service.auth.s2s"),
                                                       storeName);
        AuthDetailsConfig.S2sAuthDetailsConfig config = new AuthDetailsConfig.S2sAuthDetailsConfig();
        config.setAuthEndpoint(requiredValue(authConfig.authEndpoint(),
                                             dataStoreKey("service.auth.auth-endpoint"),
                                             storeName));
        config.setRootCertPemPath(requiredValue(tlsConfig.rootCertPemPath(),
                                                dataStoreKey("service.auth.tls.root-cert-pem-path"),
                                                storeName));
        config.setLeafCertPath(requiredValue(s2sConfig.leafCertPath(),
                                             dataStoreKey("service.auth.s2s.leaf-cert-path"),
                                             storeName));
        config.setLeafCertKeyPath(requiredValue(s2sConfig.leafCertKeyPath(),
                                                dataStoreKey("service.auth.s2s.leaf-cert-key-path"),
                                                storeName));
        config.setIntermediateCertPath(requiredValue(s2sConfig.intermediateCertPath(),
                                                     dataStoreKey("service.auth.s2s.intermediate-cert-path"),
                                                     storeName));
        config.setTenantId(requiredValue(s2sConfig.tenantId(),
                                         dataStoreKey("service.auth.s2s.tenant-id"),
                                         storeName));
        s2sConfig.keyPassphrase().ifPresent(config::setKeyPassphrase);
        tlsConfig.certReloadDuration().ifPresent(config::setCertReloadDuration);
        tlsConfig.certSslAlgorithm().ifPresent(config::setCertSslAlgorithm);
        return config;
    }

    private static AuthDetailsConfig.OverriddenAuthDetailsConfig toKiabLocalAuthConfig() {
        AuthDetailsConfig.OverriddenAuthDetailsConfig config = new AuthDetailsConfig.OverriddenAuthDetailsConfig();
        config.setAuthProviderOverride(OfflineAuth.authProvider());
        return config;
    }

    private static AuthDetailsConfig.OverriddenAuthDetailsConfig toOverriddenAuthConfig(
            KievServiceAuthConfig authConfig,
            StoreDefinition store) {
        BasicAuthenticationDetailsProvider authProvider = store.authProvider().get()
                .orElseThrow(() -> new IllegalStateException(dataStoreKey("service.auth.type")
                                                                     + "=OVERRIDDEN requires "
                                                                     + "BasicAuthenticationDetailsProvider "
                                                                     + "to be available"
                                                                     + storeContext(store.storeName())));
        return toOverriddenAuthConfig(authProvider, authConfig.tls(), store.storeName(), store.serviceRegistry());
    }

    private static AuthDetailsConfig.OverriddenAuthDetailsConfig toOverriddenAuthConfig(
            BasicAuthenticationDetailsProvider authProvider,
            Optional<KievServiceTlsConfig> tlsConfig,
            String storeName,
            ServiceRegistry serviceRegistry) {
        AuthDetailsConfig.OverriddenAuthDetailsConfig config = new AuthDetailsConfig.OverriddenAuthDetailsConfig();
        config.setAuthProviderOverride(authProvider);
        tlsConfig.map(it -> toDynamicSslContextProviderConfig(it, storeName, serviceRegistry))
                .ifPresent(config::setDynamicSslContextProviderConfig);
        return config;
    }

    private static DynamicSslContextProviderConfig toDynamicSslContextProviderConfig(KievServiceTlsConfig tlsConfig,
                                                                                    String storeName,
                                                                                    ServiceRegistry serviceRegistry) {
        Optional<String> providerName = tlsConfig.dynamicSslContextProviderName();
        if (providerName.isPresent()) {
            String name = providerName.get();
            if (name.isBlank()) {
                throw new IllegalStateException(missingConfigMessage(
                        dataStoreKey("service.auth.tls.dynamic-ssl-context-provider-name"), storeName));
            }
            return serviceRegistry.getNamed(DynamicSslContextProviderConfig.class, name);
        }

        DynamicSslContextProviderConfig config = new DynamicSslContextProviderConfig();
        config.setRootCertPath(requiredValue(tlsConfig.rootCertPemPath(),
                                             dataStoreKey("service.auth.tls.root-cert-pem-path"),
                                             storeName));
        tlsConfig.certReloadDuration().ifPresent(config::setDuration);
        tlsConfig.certSslAlgorithm().ifPresent(config::setSslAlgorithm);
        return config;
    }

    private static void validateRequired(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " must be configured");
        }
    }

    private static void validateRequired(String value, String key, String storeName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(missingConfigMessage(key, storeName));
        }
    }

    private static <T> T requiredValue(java.util.Optional<T> value, String key, String storeName) {
        return value.orElseThrow(() -> new IllegalStateException(missingConfigMessage(key, storeName)));
    }

    private static String dataStoreKey(String suffix) {
        return DATA_STORES_CONFIG_KEY + "." + suffix;
    }

    private static String missingConfigMessage(String key, String storeName) {
        return key + " must be configured" + storeContext(storeName);
    }

    private static String missingBackendConfigMessage(String key, KievBackend backend, String storeName) {
        return key + " must be configured for " + backend + " backend" + storeContext(storeName);
    }

    private static String storeContext(String storeName) {
        return storeName == null || storeName.isBlank() ? "" : " for store-name '" + storeName + "'";
    }

    private record StoreDefinition(KievBackend backend,
                                   String storeName,
                                   String appName,
                                   int transactionMaxReads,
                                   int transactionMaxWrites,
                                   java.util.Optional<KievDirectDbConfig> directDb,
                                   java.util.Optional<KievServiceConfig> service,
                                   Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
                                   ServiceRegistry serviceRegistry) {
    }
}
