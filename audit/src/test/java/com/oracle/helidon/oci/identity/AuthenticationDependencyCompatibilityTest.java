/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.nio.file.Path;
import java.security.Security;

import io.helidon.config.Config;
import io.helidon.service.registry.Services;

import com.oracle.jipher.provider.JipherJCE;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/*
 * This audit-module test uses the identity package so it can exercise the
 * package-private identity factories which reproduce the deployed failure path.
 */
class AuthenticationDependencyCompatibilityTest {

    static {
        Security.addProvider(new JipherJCE());
    }

    @Test
    void testAuthenticatorClientCreationUsesCompatibleAuthenticationClasses() {
        ServiceAuthenticationClient serviceAuthClient = new ServiceAuthenticationClientFactory(
                identityConfigFactory(authenticationConfig(true), authorizationConfig()),
                locationDefaults())
                .get();
        Services.set(ServiceAuthenticationClient.class, serviceAuthClient);

        AuthenticatorClient client = new AuthenticatorClientFactory(
                identityConfigFactory(authenticationConfig(false), authorizationConfig()),
                locationDefaults())
                .get();

        assertThat(client, notNullValue());
    }

    private static IdentityConfigFactory identityConfigFactory(AuthenticationConfig authenticationConfig,
                                                              AuthorizationConfig authorizationConfig) {
        return new IdentityConfigFactory(Config.empty()) {
            @Override
            public IdentityConfig get() {
                return new IdentityConfig() {
                    @Override
                    public AuthenticationConfig authentication() {
                        return authenticationConfig;
                    }

                    @Override
                    public AuthorizationConfig authorization() {
                        return authorizationConfig;
                    }

                    @Override
                    public SplatAwareConfig splatAware() {
                        return SplatAwareConfig.builder().build();
                    }
                };
            }
        };
    }

    private static OciEnvLocationDefaults locationDefaults() {
        return new OciEnvLocationDefaults(Config.empty(), () -> {
            throw new AssertionError("Default region should not be resolved when an explicit region is configured");
        });
    }

    private static AuthenticationConfig authenticationConfig(boolean hardCodedKeys) {
        AuthenticationConfig.Builder builder = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .region("us-phoenix-1")
                .rootCertPath(testRootCertPath())
                .hardCodedKeySupplier(hardCodedKeys);

        if (hardCodedKeys) {
            builder.useInstancePrincipal(false);
        }

        return builder.build();
    }

    private static AuthorizationConfig authorizationConfig() {
        return AuthorizationConfig.builder()
                .region("us-phoenix-1")
                .serviceName("service")
                .rootCertPath(testRootCertPath())
                .physicalAd("AD-1")
                .build();
    }

    private static String testRootCertPath() {
        return Path.of("src", "test", "resources", "serverCert.pem")
                .toAbsolutePath()
                .toString();
    }
}
