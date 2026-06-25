/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.auth.AuthMetricsFactory;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
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
@Weight(Weighted.DEFAULT_WEIGHT - 30)
public class AuthenticatorClientFactory implements Supplier<AuthenticatorClient> {

    private final AuthenticationConfig config;
    private final OciEnvLocationDefaults locationDefaults;

    @Service.Inject
    AuthenticatorClientFactory(IdentityConfigFactory config, OciEnvLocationDefaults locationDefaults) {
        this.config = config.get().authentication();
        this.locationDefaults = locationDefaults;
    }

    @Override
    public AuthenticatorClient get() {
        // get instance of service auth client
        ServiceAuthenticationClient serviceAuthClient = Services.get(ServiceAuthenticationClient.class);

        Optional<String> metricsLib = config.metricsLib();
        URI serviceUri = resolveServiceUri();
        return new AuthenticatorClient.Builder()
                .keyServiceUrl(serviceUri)
                .authMetrics(metricsLib.isEmpty() ? new NoopAuthMetricsImpl()
                                     : AuthMetricsFactory.getInstance(metricsLib.get()))
                .serviceAuthenticationClient(serviceAuthClient)
                .build();
    }

    private URI resolveServiceUri() {
        if (config.serviceUri().isPresent()) {
            if (config.region().isPresent()) {
                throw new IllegalStateException(
                        "authentication.region must not be configured when authentication.serviceUri is configured");
            }
            return config.serviceUri().orElseThrow();
        }

        Region region = locationDefaults.resolveRegion(
                config.region(),
                "One of authentication.serviceUri, authentication.region, or default region must be available");
        return locationDefaults.authServiceUri(region);
    }
}
