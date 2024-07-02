/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.secret;

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.config.Config;
import io.helidon.common.tls.TlsManager;

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
