/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

import com.oracle.pic.commons.util.AvailabilityDomain;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.auth.AuthMetricsFactory;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.metrics.NoopAuthMetricsImpl;
import com.oracle.pic.identity.authorization.common.Constants;
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
        configureEndpoint(client);

        return Optional.of(client.build());
    }

    private void configureEndpoint(AuthorizationClient.Builder client) {
        if (config.serviceUri().isPresent()) {
            configureExplicitEndpoint(client);
            return;
        }

        if (config.serviceEnclave()) {
            if (config.region().isPresent()) {
                throw new IllegalStateException(
                        "authorization.region must not be set when authorization.serviceEnclave is true");
            }
            if (config.availabilityDomain().isEmpty()) {
                throw new IllegalStateException(
                        "authorization.availabilityDomain must be configured when authorization.serviceEnclave "
                                + "is true and no explicit serviceUri is provided");
            }

            client.serviceEnclave();
            client.availabilityDomain(AvailabilityDomain.fromName(config.availabilityDomain().orElseThrow()));
            return;
        }

        if (config.region().isEmpty()) {
            throw new IllegalStateException(
                    "authorization.region must be configured when authorization.serviceUri is not provided");
        }
        if (config.physicalAd().isEmpty()) {
            throw new IllegalStateException(
                    "authorization.physicalAd must be configured for non-service-enclave authorization");
        }

        client.region(Region.fromPublicRegionName(config.region().orElseThrow()));
    }

    private void configureExplicitEndpoint(AuthorizationClient.Builder client) {
        String endpoint = config.serviceUri().orElseThrow().toString();
        client.authorizationEndpoint(endpoint);

        boolean explicitServiceEnclaveEndpoint = isServiceEnclaveEndpoint(endpoint);
        if (explicitServiceEnclaveEndpoint) {
            if (config.region().isPresent()) {
                throw new IllegalStateException(
                        "authorization.region must not be set when authorization.serviceUri points to "
                                + "a service-enclave endpoint");
            }
            if (config.availabilityDomain().isPresent()) {
                throw new IllegalStateException(
                        "authorization.availabilityDomain is redundant when authorization.serviceUri "
                                + "is explicitly configured");
            }
            if (config.physicalAd().isPresent()
                    && !Constants.REGIONAL_AD_VALUE.equalsIgnoreCase(config.physicalAd().orElseThrow())) {
                throw new IllegalStateException(
                        "authorization.physicalAd must be omitted or set to the regional AD value when "
                                + "authorization.serviceUri points to a service-enclave endpoint");
            }
            if (config.serviceEnclave()) {
                client.serviceEnclave();
            }
            return;
        }

        if (config.serviceEnclave()) {
            throw new IllegalStateException(
                    "authorization.serviceEnclave must not be set when authorization.serviceUri points "
                            + "to a non-service-enclave endpoint");
        }
        if (config.region().isEmpty()) {
            throw new IllegalStateException(
                    "authorization.region must be configured when authorization.serviceUri points to "
                            + "a non-service-enclave endpoint");
        }
        if (config.physicalAd().isEmpty()) {
            throw new IllegalStateException(
                    "authorization.physicalAd must be configured when authorization.serviceUri points to "
                            + "a non-service-enclave endpoint");
        }

        client.region(Region.fromPublicRegionName(config.region().orElseThrow()));
    }

    private boolean isServiceEnclaveEndpoint(String endpoint) {
        String host = java.net.URI.create(endpoint).getHost();
        return host != null && host.toLowerCase().startsWith("authservice");
    }
}
