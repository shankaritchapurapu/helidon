/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.config.Config;
import io.helidon.common.tls.TlsManager;

/**
 * The OCI Certificates contract of {@link io.helidon.common.tls.TlsManager}. The implementation should load/create
 * {@link io.helidon.common.tls.Tls} instances from integrating to the certificates stored remotely in OCI's
 * internal SSV2 and PKI Services, and then allow for a scheduled update check of the Tls instance for changes.
 */
@RuntimeType.PrototypedBy(SecretServiceTlsManagerConfig.class)
public interface SecretServiceTlsManager extends TlsManager, RuntimeType.Api<SecretServiceTlsManagerConfig> {
    /**
     * Creates a default {@link SecretServiceTlsManager} instance.
     *
     * @return a default instance
     */
    static SecretServiceTlsManager create() {
        return builder().build();
    }

    /**
     * Creates a configured {@link SecretServiceTlsManager} instance.
     *
     * @param config the config
     * @return a configured instance
     */
    static SecretServiceTlsManager create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Creates a configured {@link SecretServiceTlsManager} instance.
     *
     * @param cfg the config
     * @return a configured instance
     */
    static SecretServiceTlsManager create(SecretServiceTlsManagerConfig cfg) {
        return new DefaultSecretServiceTlsManager(cfg);
    }

    /**
     * Creates a {@link SecretServiceTlsManager} builder instance.
     *
     * @return a builder instance
     */
    static SecretServiceTlsManagerConfig.Builder builder() {
        return SecretServiceTlsManagerConfig.builder();
    }

    /**
     * Creates a consumer based {@link SecretServiceTlsManager} instance.
     *
     * @param consumer the consumer
     * @return a consumer based instance
     */
    static SecretServiceTlsManager create(Consumer<SecretServiceTlsManagerConfig.Builder> consumer) {
        var builder = SecretServiceTlsManagerConfig.builder();
        consumer.accept(builder);
        return builder.build();
    }
}
