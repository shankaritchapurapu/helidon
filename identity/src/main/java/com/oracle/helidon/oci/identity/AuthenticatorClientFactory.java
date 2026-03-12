/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

import com.oracle.pic.identity.auth.AuthMetricsFactory;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.key.WarnHardCodedRSAPublicKeySupplier;
import com.oracle.pic.identity.authentication.metrics.NoopAuthMetricsImpl;

/**
 * A factory for creating configured {@link AuthenticatorClient} instances.
 * <p>
 * This factory is registered as a {@link Service.Singleton} so that a single,
 * shared instance is used within the application. It derives its settings
 * from {@link AuthenticationConfig}, which in turn is obtained from the
 * provided {@link IdentityConfigFactory}.
 */
@Service.Singleton
public class AuthenticatorClientFactory implements Supplier<AuthenticatorClient> {

    private static final String SERVICE_URI = "https://auth.%s.oraclecloud.com";

    private final AuthenticationConfig config;

    AuthenticatorClientFactory(IdentityConfigFactory config) {
        this.config = config.get().authentication();
    }

    @Override
    public AuthenticatorClient get() {
        // get instance of service auth client
        ServiceAuthenticationClient serviceAuthClient = Services.get(ServiceAuthenticationClient.class);

        // check if hardcoded keys
        if (config.hardCodedKeySupplier()) {
            return new AuthenticatorClient.Builder()
                    .keySupplier(new WarnHardCodedRSAPublicKeySupplier())
                    .withNoAuthMetrics()
                    .serviceAuthenticationClient(serviceAuthClient)
                    .build();
        }

        // otherwise connect to auth endpoint
        Optional<String> metricsLib = config.metricsLib();
        URI serviceUri = config.serviceUri().orElse(URI.create(String.format(SERVICE_URI, config.region())));
        return new AuthenticatorClient.Builder()
                .keyServiceUrl(serviceUri)
                .authMetrics(metricsLib.isEmpty() ? new NoopAuthMetricsImpl()
                                     : AuthMetricsFactory.getInstance(metricsLib.get()))
                .serviceAuthenticationClient(serviceAuthClient)
                .build();
    }
}
