/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import io.helidon.common.tls.TlsManager;
import io.helidon.common.tls.spi.TlsManagerProvider;
import io.helidon.config.Config;

/**
 * Service provider for {@link DefaultSecretServiceTlsManager}.
 */
public class DefaultSecretServiceTlsManagerProvider implements TlsManagerProvider {
    @Override
    public String configKey() {
        return DefaultSecretServiceTlsManager.TYPE;
    }

    @Override
    public TlsManager create(Config config, String name) {
        SecretServiceTlsManagerConfig cfg = SecretServiceTlsManagerConfig.create(config);
        return new DefaultSecretServiceTlsManager(cfg, name);
    }
}
