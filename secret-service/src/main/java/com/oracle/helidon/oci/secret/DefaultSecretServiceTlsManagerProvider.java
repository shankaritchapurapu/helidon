/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import io.helidon.common.config.Config;
import io.helidon.common.tls.TlsManager;
import io.helidon.common.tls.spi.TlsManagerProvider;

/**
 * The service provider for DefaultSecretServiceTlsManager.
 */
public class DefaultSecretServiceTlsManagerProvider implements TlsManagerProvider {

    /**
     * Service loader based constructor.
     *
     * @deprecated this is a Java ServiceLoader implementation and the constructor should not be used directly
     */
    public DefaultSecretServiceTlsManagerProvider() {
    }

    @Override
    public String configKey() {
        return DefaultSecretServiceTlsManager.TYPE;
    }

    @Override
    public TlsManager create(Config config, String name) {
        SecretServiceTlsManagerConfig cfg = SecretServiceTlsManagerConfig.create(config);
        return TlsManagerProvider.getOrCreate(cfg, (c) -> new DefaultSecretServiceTlsManager(cfg, name, config));
    }

}
