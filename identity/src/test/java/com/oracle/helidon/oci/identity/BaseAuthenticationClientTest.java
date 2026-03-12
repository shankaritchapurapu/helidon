/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.security.Security;

import io.helidon.config.Config;

import com.oracle.jipher.provider.JipherJCE;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;

class BaseAuthenticationClientTest {

    static {
        Security.addProvider(new JipherJCE());
    }

    AuthenticationConfig authenticationConfig(boolean hardCodedKeys) {
        return AuthenticationConfig.builder()
                .globalBusinessUnit("gbu")
                .teamName("team")
                .applicationName("app")
                .region("us-phoenix-1")
                .hardCodedKeySupplier(hardCodedKeys)
                .useInstancePrincipal(false)
                .build();
    }

    AuthorizationConfig authorizationConfig() {
        return AuthorizationConfig.builder()
                .region("us-phoenix-1")
                .serviceName("service")
                .physicalAd("AD-1")
                .build();
    }

    IdentityConfigFactory identityConfigFactory() {
        return identityConfigFactory(false);
    }

    IdentityConfigFactory identityConfigFactory(boolean hardCodedKeys) {
        Config config = Config.empty();
        return new IdentityConfigFactory(config) {
            @Override
            public IdentityConfig get() {
                return new IdentityConfig() {
                    @Override
                    public AuthenticationConfig authentication() {
                        return authenticationConfig(hardCodedKeys);
                    }

                    @Override
                    public AuthorizationConfig authorization() {
                        return authorizationConfig();
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
                identityConfigFactory(hardCodedKeys));
        return clientFactory.get();
    }
}
