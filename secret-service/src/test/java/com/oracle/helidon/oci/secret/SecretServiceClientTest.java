/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SecretServiceClientTest {
    @Test
    void urlResolution() {
        SecretServiceClient secretServiceClient = SecretServiceClient
                .create(Config.builder()
                                .addSource(ConfigSources.create(
                                        Map.of("endpoint",
                                               "https://secret-service-ce.<<region>>.oracleiaas.com/v1")))
                                .build());
        secretServiceClient.initClient();
        assertThat(secretServiceClient.getSecretServiceConfig().getEndpoint(),
                   is("https://secret-service-ce." + RegionTestProvider.REGION.getRegionId() + ".oracleiaas.com/v1"));
    }
}
