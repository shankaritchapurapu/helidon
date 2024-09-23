/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import io.helidon.common.config.Config;
import io.helidon.common.configurable.ResourceConfig;
import io.helidon.common.tls.TlsManager;
import io.helidon.common.tls.spi.TlsManagerProvider;

import org.eclipse.microprofile.config.ConfigProvider;

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
        // FIXME: Revert when https://github.com/helidon-io/helidon/issues/9265 is fixed
        String enabled = ConfigProvider.getConfig().getConfigValue("server.tls.enabled").getValue();
        SecretServiceTlsManagerConfig cfg;
        if (enabled != null && enabled.equals("false")) {
            cfg = SecretServiceTlsManagerConfig.builder()
                    .reload(ReloadConfig.create())
                    .pki(PkiConfig.create())
                    .trust(ResourceConfig.builder().content("").build())
                    .buildPrototype();
        } else {
            cfg = SecretServiceTlsManagerConfig.create(config);
        }
        return TlsManagerProvider.getOrCreate(cfg, (c) -> new DefaultSecretServiceTlsManager(cfg, name, config));
    }

}
