/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package com.oracle.helidon.oci.identity;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class OciIdentityConfigurationTest {

    @Test
    void appConfigDefaults() {
        assertThat(OciIdentityConfiguration.globalAppConfig().key().name(), equalTo("oci-app"));
        OciIdentityConfiguration.AppConfig appConfig = OciIdentityConfiguration.appConfig();
        assertThat(appConfig.name(), equalTo("xxxx"));
        assertThat(appConfig.teamName(), equalTo("x-team"));
        assertThat(appConfig.stage(), equalTo("DEVELOPMENT"));
        assertThat(appConfig.isDevStage(), is(true));
        assertThat(appConfig.globalBusinessUnit(), equalTo("GBU"));
    }

    @Test
    void authConfigDefaults() {
        assertThat(OciIdentityConfiguration.globalOciIdentityConfig().key().name(), equalTo("oci-identity"));
        OciIdentityConfiguration.AuthConfig authConfig = OciIdentityConfiguration.authConfig();
        assertThat(authConfig.authEnabled(), is(false));
        assertThat(authConfig.authServiceEndpoint().toString(), equalTo("https://auth.us-phoenix-1.oraclecloud.com"));
        assertThat(authConfig.rootCertPath(), equalTo("/etc/oci-pki/ca-bundle.pem"));
        // note: this file actually exists for the build-service environment
//        assertThat(authConfig.rootCertFilePath().isPresent(), is(false));
        assertThat(authConfig.metricsEnabled(), is(false));
        assertThat(authConfig.metadataEndpoint().toString(), equalTo("http://localhost:8080"));
        assertThat(authConfig.refreshAuthTokenBeforeSecondsToExpire(), is(5));
    }

    @Test
    void appConfigPopulated() {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("name", "name",
                                                     "teamName", "teamName",
                                                     "stage", OciIdentityConfiguration.AppConfig.TEST_STAGE,
                                                     "globalBusinessUnit", "GBU")))
                .build();

        OciIdentityConfiguration.AppConfig appConfig = new OciIdentityConfiguration.AppConfig(config);
        assertThat(appConfig.name(), equalTo("name"));
        assertThat(appConfig.teamName(), equalTo("teamName"));
        assertThat(appConfig.stage(), equalTo("TEST"));
        assertThat(appConfig.isDevStage(), is(false));
        assertThat(appConfig.globalBusinessUnit(), equalTo("GBU"));
    }

    @Test
    void authConfigPopulated() {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("authEnabled", "true",
                                                     "metricsEnabled", "true",
                                                     "authServiceEndpoint", "authServiceEndpoint",
                                                     "metadataEndpoint", "metadataEndpoint",
                                                     "refreshAuthTokenBeforeSecondsToExpire", "1",
                                                     "rootCertPath", "rootCertPath")))
                .build();

        OciIdentityConfiguration.AuthConfig authConfig = new OciIdentityConfiguration.AuthConfig(config);
        assertThat(authConfig.authEnabled(), is(true));
        assertThat(authConfig.authServiceEndpoint().toString(), equalTo("authServiceEndpoint"));
        assertThat(authConfig.rootCertPath(), equalTo("rootCertPath"));
        assertThat(authConfig.rootCertFilePath().isPresent(), is(false));
        assertThat(authConfig.metricsEnabled(), is(true));
        assertThat(authConfig.metadataEndpoint().toString(), equalTo("metadataEndpoint"));
        assertThat(authConfig.refreshAuthTokenBeforeSecondsToExpire(), is(1));
    }

}
