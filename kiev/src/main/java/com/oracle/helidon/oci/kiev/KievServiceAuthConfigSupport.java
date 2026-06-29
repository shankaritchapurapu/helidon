/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import io.helidon.builder.api.Prototype;

import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalAuthConfig;

/**
 * Builder decorator for Kiev service authentication defaults.
 */
final class KievServiceAuthConfigSupport
        implements Prototype.BuilderDecorator<KievServiceAuthConfig.BuilderBase<?, ?>> {
    private static final String USE_PLATFORM_PROVIDED = "service-principal.use-platform-provided";

    KievServiceAuthConfigSupport() {
    }

    @Override
    public void decorate(KievServiceAuthConfig.BuilderBase<?, ?> builder) {
        if (builder.type() != KievAuthType.S2S) {
            return;
        }
        builder.config()
                .filter(config -> !config.get(USE_PLATFORM_PROVIDED).exists())
                .ifPresent(config -> applyExplicitServicePrincipalDefault(builder));
    }

    private static void applyExplicitServicePrincipalDefault(KievServiceAuthConfig.BuilderBase<?, ?> builder) {
        builder.servicePrincipal()
                .map(servicePrincipal -> ServicePrincipalAuthConfig.builder(servicePrincipal)
                        .usePlatformProvided(false)
                        .build())
                .ifPresent(builder::servicePrincipal);
    }
}
