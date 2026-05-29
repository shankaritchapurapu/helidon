/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.nio.file.Path;
import java.security.Security;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.jipher.provider.JipherJCE;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;

class BaseAuthenticationClientTest {

    private static final Region DEFAULT_REGION = Region.fromPublicRegionName("us-ashburn-1");

    static {
        Security.addProvider(new JipherJCE());
    }

    AuthenticationConfig authenticationConfig(boolean hardCodedKeys) {
        AuthenticationConfig.Builder builder = AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .region("us-phoenix-1")
                .rootCertPath(testRootCertPath())
                .hardCodedKeySupplier(hardCodedKeys);

        if (hardCodedKeys) {
            builder.useInstancePrincipal(false);
        } else {
            // Use local test certificates for the non-hardcoded branch so unit tests
            // do not depend on instance principal metadata access.
            builder.useInstancePrincipal(false)
                    .certificates(List.of(AuthCertificateConfig.builder()
                                                  .certificate("serverCert.pem")
                                                  .privateKey("serverKey.pem")
                                                  .build()));
        }

        return builder.build();
    }

    AuthorizationConfig authorizationConfig() {
        return AuthorizationConfig.builder()
                .region("us-phoenix-1")
                .serviceName("service")
                .rootCertPath(testRootCertPath())
                .physicalAd("AD-1")
                .build();
    }

    IdentityConfigFactory identityConfigFactory() {
        return identityConfigFactory(false);
    }

    IdentityConfigFactory identityConfigFactory(boolean hardCodedKeys) {
        return identityConfigFactory(authenticationConfig(hardCodedKeys), authorizationConfig());
    }

    IdentityConfigFactory identityConfigFactory(AuthenticationConfig authenticationConfig,
                                                AuthorizationConfig authorizationConfig) {
        Config config = Config.empty();
        return new IdentityConfigFactory(config) {
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

    ServiceAuthenticationClient serviceAuthenticationClient() {
        return serviceAuthenticationClient(false);
    }

    ServiceAuthenticationClient serviceAuthenticationClient(boolean hardCodedKeys) {
        ServiceAuthenticationClientFactory clientFactory = new ServiceAuthenticationClientFactory(
                identityConfigFactory(hardCodedKeys), ociEnvLocationDefaults(failingDefaultRegion()));
        return clientFactory.get();
    }

    OciEnvLocationDefaults ociEnvLocationDefaults(Supplier<Optional<Region>> defaultRegion) {
        return ociEnvLocationDefaults(Config.empty(), defaultRegion);
    }

    OciEnvLocationDefaults ociEnvLocationDefaults(Config config, Supplier<Optional<Region>> defaultRegion) {
        return new OciEnvLocationDefaults(config, defaultRegion);
    }

    Config ociEnvLocationConfig() {
        return Config.just(ConfigSources.create(java.util.Map.of(
                "oci.env.availability-domain", "iad-ad-1",
                "oci.env.fault-domain", "2")));
    }

    Supplier<Optional<Region>> defaultRegion() {
        return () -> Optional.of(DEFAULT_REGION);
    }

    Supplier<Optional<Region>> defaultRegion(AtomicBoolean called) {
        return () -> {
            called.set(true);
            return Optional.of(DEFAULT_REGION);
        };
    }

    Supplier<Optional<Region>> emptyDefaultRegion() {
        return Optional::empty;
    }

    Supplier<Optional<Region>> failingDefaultRegion() {
        return () -> {
            throw new AssertionError("Default region should not be resolved when an explicit region or endpoint is configured");
        };
    }

    String testRootCertPath() {
        return Path.of("src/test/resources/serverCert.pem").toAbsolutePath().toString();
    }
}
