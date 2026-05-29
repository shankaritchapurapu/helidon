/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

final class DynamicSslProviderConfigSupport
        implements Prototype.BuilderDecorator<DynamicSslProviderConfig.BuilderBase<?, ?>> {
    private static final String NAME = "name";
    private static final String ROOT_CERT_PATH = "root-cert-path";
    private static final String ROOT_CERT_PEM_PATH = "root-cert-pem-path";
    private static final String DURATION = "duration";
    private static final String CERT_RELOAD_DURATION = "cert-reload-duration";
    private static final String SSL_ALGORITHM = "ssl-algorithm";
    private static final String CERT_SSL_ALGORITHM = "cert-ssl-algorithm";

    DynamicSslProviderConfigSupport() {
    }

    @Override
    public void decorate(DynamicSslProviderConfig.BuilderBase<?, ?> builder) {
        builder.config().ifPresent(config -> applyAliases(config, builder));
        rejectBlankName(builder);
        rejectBlankRootCertPath(builder);
    }

    private static void applyAliases(Config config, DynamicSslProviderConfig.BuilderBase<?, ?> builder) {
        applyRootCertAlias(config, builder);
        applyDurationAlias(config, builder);
        applySslAlgorithmAlias(config, builder);
    }

    private static void applyRootCertAlias(Config config, DynamicSslProviderConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(ROOT_CERT_PATH).exists();
        boolean aliasExists = config.get(ROOT_CERT_PEM_PATH).exists();
        rejectBoth(canonicalExists, aliasExists, ROOT_CERT_PATH, ROOT_CERT_PEM_PATH);
        if (aliasExists) {
            builder.rootCertPemPath().ifPresent(builder::rootCertPath);
        }
    }

    private static void applyDurationAlias(Config config, DynamicSslProviderConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(DURATION).exists();
        boolean aliasExists = config.get(CERT_RELOAD_DURATION).exists();
        rejectBoth(canonicalExists, aliasExists, DURATION, CERT_RELOAD_DURATION);
        if (aliasExists) {
            builder.certReloadDuration().ifPresent(builder::duration);
        }
    }

    private static void applySslAlgorithmAlias(Config config, DynamicSslProviderConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(SSL_ALGORITHM).exists();
        boolean aliasExists = config.get(CERT_SSL_ALGORITHM).exists();
        rejectBoth(canonicalExists, aliasExists, SSL_ALGORITHM, CERT_SSL_ALGORITHM);
        if (aliasExists) {
            builder.certSslAlgorithm().ifPresent(builder::sslAlgorithm);
        }
    }

    private static void rejectBoth(boolean canonicalExists, boolean aliasExists, String canonical, String alias) {
        if (canonicalExists && aliasExists) {
            throw new IllegalArgumentException("Do not configure both " + canonical
                                                       + " and " + alias + "; specify only one.");
        }
    }

    private static void rejectBlankName(DynamicSslProviderConfig.BuilderBase<?, ?> builder) {
        builder.name()
                .filter(String::isBlank)
                .ifPresent(name -> {
                    throw new IllegalArgumentException(NAME + " must not be blank");
                });
    }

    private static void rejectBlankRootCertPath(DynamicSslProviderConfig.BuilderBase<?, ?> builder) {
        builder.rootCertPath()
                .filter(String::isBlank)
                .ifPresent(name -> {
                    throw new IllegalArgumentException(ROOT_CERT_PATH + " must not be blank");
                });
    }
}
