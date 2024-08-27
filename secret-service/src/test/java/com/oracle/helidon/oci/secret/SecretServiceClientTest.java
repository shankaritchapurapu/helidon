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
