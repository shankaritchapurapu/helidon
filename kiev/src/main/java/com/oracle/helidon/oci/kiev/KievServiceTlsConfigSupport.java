/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

final class KievServiceTlsConfigSupport implements Prototype.BuilderDecorator<KievServiceTlsConfig.BuilderBase<?, ?>> {
    private static final String ROOT_CERT_PEM_PATH = "root-cert-pem-path";
    private static final String ROOT_CERT_PATH = "root-cert-path";

    KievServiceTlsConfigSupport() {
    }

    @Override
    public void decorate(KievServiceTlsConfig.BuilderBase<?, ?> builder) {
        builder.config().ifPresent(config -> applyRootCertPathAlias(config, builder));
    }

    private static void applyRootCertPathAlias(Config config, KievServiceTlsConfig.BuilderBase<?, ?> builder) {
        boolean canonicalExists = config.get(ROOT_CERT_PEM_PATH).exists();
        boolean aliasExists = config.get(ROOT_CERT_PATH).exists();
        if (canonicalExists && aliasExists) {
            throw new IllegalArgumentException("Do not configure both " + ROOT_CERT_PEM_PATH
                                                       + " and " + ROOT_CERT_PATH + "; specify only one.");
        }
        if (aliasExists) {
            builder.rootCertPath().ifPresent(builder::rootCertPemPath);
        }
    }
}
