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
import com.oracle.pic.kiev.auth.AuthDetailsConfig;
import com.oracle.pic.kiev.registry.config.ClientRegistryConfig;
import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;
import com.oracle.pic.kiev.streams.service.client.config.StreamingConfig;

/**
 * Factory for creating Kiev streaming client configuration from Helidon configuration.
 */
class KievStreamingClientConfigFactory {
    private static final String DATA_STORES_CONFIG_KEY = "oci.kiev.data-stores[]";

    private KievStreamingClientConfigFactory() {
    }

    static Optional<StreamingConfig> create(KievStoreConfig storeConfig,
                                            Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
                                            ServiceRegistry serviceRegistry) {
        if (storeConfig.backend() != KievBackend.SERVICE) {
            return Optional.empty();
        }

        validateRequired(storeConfig.storeName(), dataStoreKey("store-name"));
        KievServiceConfig serviceConfig = storeConfig.service()
                .orElseThrow(() -> new IllegalStateException(missingBackendConfigMessage(dataStoreKey("service"),
                                                                                         KievBackend.SERVICE,
                                                                                         storeConfig.storeName())));
        KievServiceAuthConfig authConfig = serviceConfig.auth()
                .orElseThrow(() -> new IllegalStateException(missingBackendConfigMessage(dataStoreKey("service.auth"),
                                                                                         KievBackend.SERVICE,
                                                                                         storeConfig.storeName())));

        StreamingConfig streamingConfig = new StreamingConfig();
        streamingConfig.setStoreName(storeConfig.storeName());
        streamingConfig.setCompartmentId(serviceConfig.compartmentId());
        streamingConfig.setFrontendEndpoint(serviceConfig.frontendEndpoint());
        streamingConfig.setLocality(serviceConfig.locality().orElse(ClientRegistryLocality.REGIONAL));
        streamingConfig.setIncludeDeleteColumnValues(storeConfig.streamDeletedColumnValues());
        configureAuth(streamingConfig,
                      authConfig,
                      storeConfig.storeName(),
                      serviceConfig,
                      authProvider,
                      serviceRegistry);
        return Optional.of(streamingConfig);
    }

    private static void configureAuth(StreamingConfig streamingConfig,
                                      KievServiceAuthConfig authConfig,
                                      String storeName,
                                      KievServiceConfig serviceConfig,
                                      Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
                                      ServiceRegistry serviceRegistry) {
        switch (authConfig.type()) {
        case INSTANCE -> streamingConfig.setAuthDetailsConfig(toInstanceAuthConfig(authConfig, storeName));
        case S2S -> streamingConfig.setAuthDetailsConfig(toS2sAuthConfig(authConfig, storeName));
        case OVERRIDDEN -> streamingConfig.setAuthDetailsConfig(toOverriddenAuthConfig(authConfig,
                                                                                       storeName,
                                                                                       authProvider,
                                                                                       serviceRegistry));
        case KIAB_LOCAL -> configureKiabLocalAuth(streamingConfig, serviceConfig);
        default -> throw new IllegalStateException("Unsupported Kiev auth type " + authConfig.type());
        }
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
        return KievS2sAuthConfigFactory.create(authConfig, storeName);
    }

    private static AuthDetailsConfig.OverriddenAuthDetailsConfig toOverriddenAuthConfig(
            KievServiceAuthConfig authConfig,
            String storeName,
            Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
            ServiceRegistry serviceRegistry) {
        AuthDetailsConfig.OverriddenAuthDetailsConfig config = new AuthDetailsConfig.OverriddenAuthDetailsConfig();
        // Kiev client configuration APIs require a concrete auth provider up front, matching KaasStoreConfig.
        config.setAuthProviderOverride(authProvider.get()
                                               .orElseThrow(() -> new IllegalStateException(
                                                       dataStoreKey("service.auth.type")
                                                               + "=OVERRIDDEN requires "
                                                               + "BasicAuthenticationDetailsProvider to be available"
                                                               + storeContext(storeName))));
        authConfig.tls()
                .map(tlsConfig -> toDynamicSslContextProviderConfig(tlsConfig, storeName, serviceRegistry))
                .ifPresent(config::setDynamicSslContextProviderConfig);
        return config;
    }

    private static void configureKiabLocalAuth(StreamingConfig streamingConfig, KievServiceConfig serviceConfig) {
        AuthDetailsConfig.OverriddenAuthDetailsConfig config = new AuthDetailsConfig.OverriddenAuthDetailsConfig();
        config.setAuthProviderOverride(OfflineAuth.authProvider());
        streamingConfig.setAuthDetailsConfig(config);
        streamingConfig.setRegistryConfig(ClientRegistryConfig.builder()
                                                  .enabled(false)
                                                  .endpointOverride(serviceConfig.frontendEndpoint())
                                                  .build());
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
}
