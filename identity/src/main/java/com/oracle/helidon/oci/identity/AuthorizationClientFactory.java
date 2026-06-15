/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
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
@Weight(Weighted.DEFAULT_WEIGHT - 30)
public class AuthorizationClientFactory implements Supplier<Optional<IAuthorizationClient>> {

    private static final System.Logger LOGGER = System.getLogger(AuthorizationClientFactory.class.getName());

    private final AuthorizationConfig config;
    private final OciEnvLocationDefaults locationDefaults;

    @Service.Inject
    AuthorizationClientFactory(IdentityConfigFactory factory, OciEnvLocationDefaults locationDefaults) {
        this.config = factory.get().authorization();
        this.locationDefaults = locationDefaults;
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
            Optional<String> physicalAd = config.physicalAd();
            if (physicalAd.isPresent()) {
                String value = physicalAd.orElseThrow();
                if (Constants.REGIONAL_AD_VALUE.equalsIgnoreCase(value)) {
                    client.physicalAD(value);
                } else {
                    // Service-enclave endpoints derive placement from availability-domain; warn and ignore AD-specific input.
                    LOGGER.log(System.Logger.Level.WARNING,
                               "Ignoring authorization.physicalAd {0} because authorization.serviceEnclave is true "
                                       + "and no authorization.serviceUri is configured",
                               value);
                }
            }
            client.serviceEnclave();
            client.availabilityDomain(AvailabilityDomain.fromName(locationDefaults.requireAvailabilityDomain(
                    config.availabilityDomain(),
                    "authorization.availabilityDomain or default availability domain must be available when "
                            + "authorization.serviceEnclave is true and no explicit serviceUri is provided")));
            return;
        }

        Region region = locationDefaults.resolveRegion(
                config.region(),
                "authorization.region or default region must be available when authorization.serviceUri is not provided");
        // Resolve region first so a physical AD default can be checked against the same location.
        client.physicalAD(locationDefaults.requirePhysicalAd(
                config.physicalAd(),
                region,
                "authorization.physicalAd or default physical AD for authorization.region must be available for "
                        + "non-service-enclave authorization"));
        client.region(region);
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
            config.physicalAd().ifPresent(client::physicalAD);
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
        Region region = locationDefaults.resolveRegion(
                config.region(),
                "authorization.region or default region must be available when authorization.serviceUri points to "
                        + "a non-service-enclave endpoint");
        // Resolve region first so a physical AD default can be checked against the same location.
        client.physicalAD(locationDefaults.requirePhysicalAd(
                config.physicalAd(),
                region,
                "authorization.physicalAd or default physical AD for authorization.region must be available when "
                        + "authorization.serviceUri points to a non-service-enclave endpoint"));
        client.region(region);
    }

    private boolean isServiceEnclaveEndpoint(String endpoint) {
        String host = java.net.URI.create(endpoint).getHost();
        return host != null && host.toLowerCase().startsWith("authservice");
    }

}
