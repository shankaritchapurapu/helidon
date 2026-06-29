/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalAuthConfig;
import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalCertificateConfig;
import com.oracle.pic.kiev.auth.AuthDetailsConfig;

/**
 * Factory for creating Kiev S2S authentication configuration from Helidon service-principal configuration.
 */
class KievS2sAuthConfigFactory {
    private static final String DATA_STORES_CONFIG_KEY = "oci.kiev.data-stores[]";

    private KievS2sAuthConfigFactory() {
    }

    static AuthDetailsConfig.S2sAuthDetailsConfig create(KievServiceAuthConfig authConfig, String storeName) {
        KievServiceTlsConfig tlsConfig = requiredValue(authConfig.tls(),
                                                       dataStoreKey("service.auth.tls"),
                                                       storeName);
        validateS2sAuthEndpoint(authConfig, storeName);
        ServicePrincipalAuthConfig servicePrincipalConfig = requiredValue(
                authConfig.servicePrincipal(),
                dataStoreKey("service.auth.service-principal"),
                storeName);
        validateExplicitServicePrincipalConfig(servicePrincipalConfig, storeName);

        AuthDetailsConfig.S2sAuthDetailsConfig config = new AuthDetailsConfig.S2sAuthDetailsConfig();
        config.setAuthEndpoint(requiredValue(servicePrincipalConfig.federationEndpoint().map(Object::toString),
                                             dataStoreKey("service.auth.service-principal.federation-endpoint"),
                                             storeName));
        config.setRootCertPemPath(requiredValue(tlsConfig.rootCertPemPath(),
                                                dataStoreKey("service.auth.tls.root-cert-pem-path"),
                                                storeName));

        ServicePrincipalCertificateConfig leafCertificate = servicePrincipalCertificate(servicePrincipalConfig,
                                                                                       0,
                                                                                       storeName);
        ServicePrincipalCertificateConfig intermediateCertificate = servicePrincipalCertificate(servicePrincipalConfig,
                                                                                               1,
                                                                                               storeName);
        validateKievCertificateCount(servicePrincipalConfig, storeName);
        config.setLeafCertPath(leafCertificate.certificate());
        config.setLeafCertKeyPath(requiredValue(
                leafCertificate.privateKey(),
                dataStoreKey("service.auth.service-principal.certificates.0.private-key"),
                storeName));
        config.setIntermediateCertPath(intermediateCertificate.certificate());
        config.setTenantId(requiredValue(servicePrincipalConfig.tenantId(),
                                         dataStoreKey("service.auth.service-principal.tenant-id"),
                                         storeName));
        if (!leafCertificate.passphrase().isEmpty()) {
            config.setKeyPassphrase(leafCertificate.passphrase());
        }
        tlsConfig.certReloadDuration().ifPresent(config::setCertReloadDuration);
        tlsConfig.certSslAlgorithm().ifPresent(config::setCertSslAlgorithm);
        return config;
    }

    private static void validateS2sAuthEndpoint(KievServiceAuthConfig authConfig, String storeName) {
        if (authConfig.authEndpoint().isPresent()) {
            throw new IllegalStateException(dataStoreKey("service.auth.auth-endpoint")
                                                    + " must not be configured for S2S; use "
                                                    + dataStoreKey("service.auth.service-principal.federation-endpoint")
                                                    + storeContext(storeName));
        }
    }

    private static void validateExplicitServicePrincipalConfig(ServicePrincipalAuthConfig servicePrincipalConfig,
                                                               String storeName) {
        if (servicePrincipalConfig.usePlatformProvided()) {
            throw new IllegalStateException(dataStoreKey("service.auth.service-principal.use-platform-provided")
                                                    + " must be false for Kiev S2S auth"
                                                    + storeContext(storeName));
        }
    }

    private static ServicePrincipalCertificateConfig servicePrincipalCertificate(
            ServicePrincipalAuthConfig config,
            int index,
            String storeName) {

        if (config.certificates().size() <= index) {
            throw new IllegalStateException(dataStoreKey("service.auth.service-principal.certificates")
                                                    + " must contain leaf and intermediate certificate entries"
                                                    + storeContext(storeName));
        }
        return config.certificates().get(index);
    }

    private static void validateKievCertificateCount(ServicePrincipalAuthConfig config, String storeName) {
        if (config.certificates().size() > 2) {
            throw new IllegalStateException(dataStoreKey("service.auth.service-principal.certificates")
                                                    + " supports exactly leaf and intermediate certificate entries "
                                                    + "for Kiev S2S auth"
                                                    + storeContext(storeName));
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

    private static String storeContext(String storeName) {
        return storeName == null || storeName.isBlank() ? "" : " for store-name '" + storeName + "'";
    }
}
