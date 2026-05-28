/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.objectstorage;

import io.helidon.builder.api.Prototype;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.service.registry.Services;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.helidon.oci.sdk.common.OciClientConfiguration;

/**
 * Runtime config helpers for OCI Object Storage client configuration blueprints.
 */
final class ConfigSupport {
    private ConfigSupport() {
    }

    static final class ObjectStorageClientSupport
            implements Prototype.BuilderDecorator<ObjectStorageClientConfig.BuilderBase<?, ?>> {
        private static final String REGION = "region";
        private static final String REGION_ID = "region-id";

        ObjectStorageClientSupport() {
        }

        @Override
        public void decorate(ObjectStorageClientConfig.BuilderBase<?, ?> builder) {
            builder.config().ifPresent(config -> applyRegionAlias(config, builder));
        }

        @Prototype.RuntimeTypeFactoryMethod
        static ObjectStorageClient createObjectStorageClient(ObjectStorageClientConfig config) {
            ObjectStorageClient client = config.client()
                    .map(clientConfig -> new ObjectStorageClient(authProvider(), clientConfig))
                    .orElseGet(() -> new ObjectStorageClient(authProvider()));
            config.endpoint().ifPresent(client::setEndpoint);
            config.region().ifPresent(client::setRegion);
            return client;
        }

        @Prototype.ConfigFactoryMethod("client")
        static ClientConfiguration createClientConfiguration(Config config) {
            return com.oracle.helidon.oci.sdk.common.ConfigSupport.ClientConfigurationSupport
                    .createClientConfiguration(config);
        }

        @Prototype.RuntimeTypeFactoryMethod("client")
        static ClientConfiguration createClientConfiguration(OciClientConfiguration config) {
            return com.oracle.helidon.oci.sdk.common.ConfigSupport.ClientConfigurationSupport
                    .createClientConfiguration(config);
        }

        static ClientConfiguration createClientConfiguration(ClientConfiguration config) {
            return config;
        }

        private static void applyRegionAlias(io.helidon.common.config.Config config,
                                             ObjectStorageClientConfig.BuilderBase<?, ?> builder) {
            boolean canonicalExists = config.get(REGION).exists();
            boolean aliasExists = config.get(REGION_ID).exists();
            if (canonicalExists && aliasExists) {
                throw new ConfigException("Do not configure both " + REGION
                                                  + " and " + REGION_ID + "; specify only one.");
            }
            if (aliasExists) {
                builder.regionId().ifPresent(builder::region);
            }
        }
    }

    private static BasicAuthenticationDetailsProvider authProvider() {
        return Services.get(BasicAuthenticationDetailsProvider.class);
    }
}
