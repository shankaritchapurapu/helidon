/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

    @Test
    void phoenixToR2RegionConversion() {
        SecretServiceClient secretServiceClient = SecretServiceClient
                .create(Config.builder()
                                .build());
        secretServiceClient.resolveEndpoint("us-phoenix-1");
        assertThat(secretServiceClient.getSecretServiceConfig().getEndpoint(),
                   is("https://secret-service-ce.r2.oracleiaas.com/v1"));
    }

    @Test
    public void testBase64Decoding() {
        String encodedString = "SGVsbG8gV29ybGQh";
        byte[] expectedDecodedBytes = "Hello World!".getBytes();

        byte[] actualDecodedBytes = SecretServiceClient.getBase64DecodedValue(encodedString);

        assertThat(expectedDecodedBytes, is(actualDecodedBytes));
    }
}
