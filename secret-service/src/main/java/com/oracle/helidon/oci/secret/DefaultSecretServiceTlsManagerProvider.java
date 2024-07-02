/*
 * Copyright (c) 2023-2024 Oracle and/or its affiliates.
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
