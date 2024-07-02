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
package com.oracle.helidon.oci.metrics;

import java.util.concurrent.TimeUnit;

import io.helidon.config.Config;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static com.oracle.bmc.auth.AbstractFederationClientAuthenticationDetailsProviderBuilder.METADATA_SERVICE_BASE_URL;
import static jakarta.interceptor.Interceptor.Priority.LIBRARY_BEFORE;

/**
 * Loads the instance metadata late, using the active runtime config.
 * <p>
 *     The following config settings within the {@value OCI_CONFIG_PREFIX} section are customizable (default values in parens):
 *     <ul>
 *         <li>{@value #INSTANCE_METADATA_URI_CONFIG_SUFFIX}
 *      ({@value com.oracle.bmc.auth.AbstractFederationClientAuthenticationDetailsProviderBuilder#METADATA_SERVICE_BASE_URL})</li>
 *         <li>{@value #CONNECTION_TIMEOUT_CONFIG_KEY} ({@value #DEFAULT_INSTANCE_METADATA_CONNECT_TIMEOUT_MS})</li>
 *         <li>{@value #READ_TIMEOUT_CONFIG_KEY} ({@value #DEFAULT_INSTANCE_METADATA_READ_TIMEOUT_MS})</li>
 *     </ul>
 * </p>
 */
@Singleton
class InstanceMetadataLoader {

    private static final String OCI_CONFIG_PREFIX = "oci";
    private static final String INSTANCE_METADATA_PREFIX = "instance-metadata";
    private static final String INSTANCE_METADATA_URI_CONFIG_SUFFIX = INSTANCE_METADATA_PREFIX + "-uri";
    private static final long DEFAULT_INSTANCE_METADATA_CONNECT_TIMEOUT_MS = 10 * 1000;
    private static final long DEFAULT_INSTANCE_METADATA_READ_TIMEOUT_MS = 12 * 1000;
    private static final String CONNECTION_TIMEOUT_CONFIG_KEY = INSTANCE_METADATA_PREFIX + "-connection-timeout-ms";
    private static final String READ_TIMEOUT_CONFIG_KEY = INSTANCE_METADATA_PREFIX + "-read-timeout-ms";

    private JsonObject instanceMetadata;


    void init(@Observes @Priority(LIBRARY_BEFORE) @Initialized(ApplicationScoped.class) Object startup, Config config) {
        instanceMetadata = loadInstanceMetadata(config.get(OCI_CONFIG_PREFIX));
    }

    JsonObject instanceMetadata() {
        return instanceMetadata;
    }

    private JsonObject loadInstanceMetadata(Config helidonOciConfig) {
        if (!helidonOciConfig.get("metrics.enabled")
                .asBoolean()
                .orElse(false)) {
            return null;
        }
        String instanceMetadataUri = helidonOciConfig.get(INSTANCE_METADATA_URI_CONFIG_SUFFIX).asString()
                .orElse(METADATA_SERVICE_BASE_URL);
        long connectTimeout = helidonOciConfig.get(CONNECTION_TIMEOUT_CONFIG_KEY).asLong()
                .orElse(DEFAULT_INSTANCE_METADATA_CONNECT_TIMEOUT_MS);
        long readTimeout = helidonOciConfig.get(READ_TIMEOUT_CONFIG_KEY).asLong()
                .orElse(DEFAULT_INSTANCE_METADATA_READ_TIMEOUT_MS);

        try (Client client = ClientBuilder.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .build()) {
            Response response = client.target(instanceMetadataUri)
                    .path("instance")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer Oracle")
                    .get();
            if (response.getStatus() >= 300) {
                throw new RuntimeException(String.format("Cannot obtain instance metadata: status = %d, entity = '%s'",
                                                         response.getStatus(),
                                                         response.readEntity(String.class)));
            }
            return response.readEntity(JsonObject.class);
        }
    }
}
