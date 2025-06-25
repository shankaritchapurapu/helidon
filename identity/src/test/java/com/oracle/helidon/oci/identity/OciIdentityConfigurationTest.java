/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.util.List;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.common.uri.UriPath;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.PathMatcher;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.oracle.helidon.oci.identity.OciIdentityConfigurationTest.PathMatcherMatcher.pathMatches;
import static com.oracle.helidon.oci.identity.OciIdentityConfigurationTest.PathMatcherMatcher.pathNotMatches;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class OciIdentityConfigurationTest {

    @Test
    void appConfigDefaults() {
        assertThat(OciIdentityConfiguration.globalAppConfig().key().toString(), equalTo("oci.app"));
        OciIdentityConfiguration.AppConfig appConfig = OciIdentityConfiguration.appConfig();
        assertThat(appConfig.name(), equalTo("xxxx"));
        assertThat(appConfig.teamName(), equalTo("x-team"));
        assertThat(appConfig.stage(), equalTo("DEVELOPMENT"));
        assertThat(appConfig.isDevStage(), is(true));
        assertThat(appConfig.globalBusinessUnit(), equalTo("GBU"));
    }

    @Test
    void authConfigDefaults() {
        assertThat(OciIdentityConfiguration.globalOciIdentityConfig().key().toString(), equalTo("oci.identity"));
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

    @ParameterizedTest(name = "[{index}]")
    // language=yaml
    @ValueSource(strings = {
            """
                    oci.identity:
                      filters:
                        auth-context:
                          paths:
                          - path: /v1/cars
                          - path: /v1/bikes
                          - path: /v1/trains/*
                    """
            ,
            // Backward compatibility with HLDN-200 workaround
            """
                    oci.identity:
                      filters:
                        auth-context:
                          paths.path:
                          - /v1/cars
                          - /v1/bikes
                          - /v1/trains/*
                    """})
    void pathMatchersPopulated(String yaml) {
        var config = Config.builder()
                .sources(ConfigSources.create(yaml, MediaTypes.APPLICATION_X_YAML))
                .build();

        var matchers = IdentityPathMatchingRequestFilter.loadPathMatchers(config, "auth-context");

        assertThat(matchers, pathMatches("/v1/bikes"));
        assertThat(matchers, pathMatches("/v1/trains"));
        assertThat(matchers, pathMatches("/v1/trains/test"));

        assertThat(matchers, pathNotMatches("/v1/horses"));
        assertThat(matchers, pathNotMatches("/v1/bikes/test"));
        assertThat(matchers, pathNotMatches("/v2/bikes"));

    }

    static class PathMatcherMatcher extends BaseMatcher<List<PathMatcher>> {

        private final UriPath path;
        private final boolean accepted;

        static PathMatcherMatcher pathMatches(String path) {
            return new PathMatcherMatcher(path, true);
        }

        static PathMatcherMatcher pathNotMatches(String path) {
            return new PathMatcherMatcher(path, false);
        }

        PathMatcherMatcher(String path, boolean accepted) {
            this.path = UriPath.create(path);
            this.accepted = accepted;
        }

        @Override
        public boolean matches(Object item) {
            if (item instanceof List<?> list) {
                for (var it = list.iterator(); it.hasNext() && it.next() instanceof PathMatcher matcher; ) {
                    if (matcher.match(path).accepted() == accepted) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("PathMatchers ")
                    .appendText(accepted ? "matching" : "not matching")
                    .appendText(" path: ")
                    .appendValue(path);
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            description.appendText("None of the configured path matchers ")
                    .appendValue(item)
                    .appendText(" matches the path: ")
                    .appendValue(path);
        }
    }

}
