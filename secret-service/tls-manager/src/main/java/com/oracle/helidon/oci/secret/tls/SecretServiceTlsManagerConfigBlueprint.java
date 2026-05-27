/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.configurable.Resource;
import io.helidon.common.tls.spi.TlsManagerProvider;
import io.helidon.scheduling.CronConfig;

/**
 * Blueprint configuration for {@link DefaultSecretServiceTlsManager}.
 */
@Prototype.Blueprint
@Prototype.CustomMethods(SecretServiceTlsManagerConfigSupport.class)
@Prototype.Configured(value = DefaultSecretServiceTlsManager.TYPE, root = false)
@Prototype.Provides(TlsManagerProvider.class)
interface SecretServiceTlsManagerConfigBlueprint extends Prototype.Factory<SecretServiceTlsManager> {
    /**
     * Reload configuration.
     *
     * @return reload configuration
     */
    @Option.Configured("reload")
    @Option.DefaultMethod(type = SecretServiceTlsManagerConfigSupport.class, value = "defaultReload")
    CronConfig reload();

    /**
     * PKI material configuration.
     *
     * @return PKI configuration
     */
    @Option.Configured
    PkiConfig pki();

    /**
     * Trust CA bundle resource.
     *
     * @return trust CA bundle resource
     */
    @Option.Configured
    Optional<Resource> trust();
}
