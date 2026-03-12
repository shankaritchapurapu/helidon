/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.auth.AuthMetricsFactory;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.metrics.NoopAuthMetricsImpl;
import com.oracle.pic.identity.authorization.sdk.AuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;

/**
 * Factory for creating {@link IAuthorizationClient} instances based on
 * {@link AuthorizationConfig}.
 * <p>
 * This factory is registered as a {@link Service.Singleton} and is intended to be
 * injected and reused across the application. The created authorization client
 * is configured using the {@link IdentityConfig} provided by the supplied
 * {@link IdentityConfigFactory}.
 * <p>
 * If authorization is disabled in the configuration (that is,
 * {@link AuthorizationConfig#enabled()} returns {@code false}), the factory
 * returns {@link Optional#empty()} from {@link #get()} instead of creating a
 * client instance.
 */
@Service.Singleton
public class AuthorizationClientFactory implements Supplier<Optional<IAuthorizationClient>> {

    private final AuthorizationConfig config;

    AuthorizationClientFactory(IdentityConfigFactory factory) {
        this.config = factory.get().authorization();
    }

    @Override
    public Optional<IAuthorizationClient> get() {
        if (!config.enabled()) {
            return Optional.empty();
        }

        // get instance of service auth client
        ServiceAuthenticationClient serviceAuthClient = Services.get(ServiceAuthenticationClient.class);

        // create authz client builder
        Optional<String> metricsLib = config.metricsLib();
        AuthorizationClient.Builder client = new AuthorizationClient.Builder()
                .authMetrics(metricsLib.isEmpty() ? new NoopAuthMetricsImpl()
                                     : AuthMetricsFactory.getInstance(metricsLib.get()))
                .serviceAuthenticationClient(serviceAuthClient)
                .serviceName(config.serviceName());

        // set optional values
        config.rootCertPath().ifPresent(client::rootCertPath);
        config.physicalAd().ifPresent(client::physicalAD);

        // set region or service enclave
        if (config.region().isPresent()) {
            client.region(Region.fromPublicRegionName(config.region().get()));
        } else if (config.serviceEnclave()) {
            client.serviceEnclave();
        } else {
            throw new IllegalStateException("Region must be set if not running in service enclave");
        }

        return Optional.of(client.build());
    }
}
