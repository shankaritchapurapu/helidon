/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.tls.TlsManager;
import io.helidon.config.Config;

/**
 * Secret Service V2 TLS manager.
 *
 * <p>The manager loads mTLS key and certificate material from SSv2, or from a
 * local PKI JSON resource, and refreshes the X.509 key and trust managers used
 * by Helidon TLS.
 */
public interface SecretServiceTlsManager extends TlsManager, RuntimeType.Api<SecretServiceTlsManagerConfig> {
    /**
     * Creates a default manager instance.
     *
     * @return a default instance
     */
    static SecretServiceTlsManager create() {
        return builder().build();
    }

    /**
     * Creates a configured manager instance.
     *
     * @param config the config
     * @return a configured instance
     */
    static SecretServiceTlsManager create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Creates a configured manager instance.
     *
     * @param cfg manager configuration
     * @return a configured instance
     */
    static SecretServiceTlsManager create(SecretServiceTlsManagerConfig cfg) {
        return new DefaultSecretServiceTlsManager(cfg);
    }

    /**
     * Creates a manager builder.
     *
     * @return builder
     */
    static SecretServiceTlsManagerConfig.Builder builder() {
        return SecretServiceTlsManagerConfig.builder();
    }

    /**
     * Creates a manager from a builder consumer.
     *
     * @param consumer builder consumer
     * @return a configured instance
     */
    static SecretServiceTlsManager create(Consumer<SecretServiceTlsManagerConfig.Builder> consumer) {
        SecretServiceTlsManagerConfig.Builder builder = SecretServiceTlsManagerConfig.builder();
        consumer.accept(builder);
        return builder.build();
    }
}
