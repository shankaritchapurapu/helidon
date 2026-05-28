/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.pic.commons.util.Region;

@Service.Singleton
class OciEnvLocationDefaults {

    private static final String AUTH_SERVICE_URI = "https://auth.%s.%s";
    private static final String OCI_ENV_AVAILABILITY_DOMAIN = "oci.env.availability-domain";
    private static final String OCI_ENV_FAULT_DOMAIN = "oci.env.fault-domain";

    private final Config config;
    private final Supplier<Optional<Region>> defaultRegion;

    @Service.Inject
    OciEnvLocationDefaults(Config config, Supplier<Optional<Region>> defaultRegion) {
        this.config = config;
        this.defaultRegion = defaultRegion;
    }

    /**
     * Resolves an identity region using explicit identity configuration first and the
     * region provided by {@code oci-env} second.
     *
     * @param configuredRegion explicit identity region
     * @param missingMessage error message to use when neither source provides a region
     * @return resolved region
     * @throws IllegalStateException when no region is configured and {@code oci-env} has no region
     */
    Region resolveRegion(Optional<String> configuredRegion, String missingMessage) {
        if (configuredRegion.isPresent()) {
            return Region.fromPublicRegionName(configuredRegion.get());
        }
        return defaultRegion.get()
                .orElseThrow(() -> new IllegalStateException(missingMessage));
    }

    /**
     * Resolves an availability domain using explicit identity configuration first and the
     * resolved {@code oci.env.availability-domain} config value second.
     *
     * @param configuredAvailabilityDomain explicit identity availability domain
     * @return resolved availability domain, or empty when neither source provides one
     */
    Optional<String> availabilityDomain(Optional<String> configuredAvailabilityDomain) {
        if (configuredAvailabilityDomain.isPresent()) {
            return configuredAvailabilityDomain;
        }
        return stringValue(OCI_ENV_AVAILABILITY_DOMAIN);
    }

    /**
     * Resolves a required availability domain using explicit identity configuration first and
     * {@code oci.env.availability-domain} second.
     *
     * @param configuredAvailabilityDomain explicit identity availability domain
     * @param missingMessage error message to use when neither source provides an availability domain
     * @return resolved availability domain
     * @throws IllegalStateException when neither source provides an availability domain
     */
    String requireAvailabilityDomain(Optional<String> configuredAvailabilityDomain, String missingMessage) {
        return availabilityDomain(configuredAvailabilityDomain)
                .orElseThrow(() -> new IllegalStateException(missingMessage));
    }

    /**
     * Returns the resolved {@code oci.env.fault-domain} config value.
     *
     * @return fault domain from {@code oci-env}, or empty when none is configured
     */
    Optional<String> faultDomain() {
        return stringValue(OCI_ENV_FAULT_DOMAIN);
    }

    /**
     * Derives the Auth service endpoint from a region using the region's realm domain.
     *
     * @param region region used to derive the endpoint
     * @return Auth service endpoint URI
     * @throws IllegalStateException when the region realm has no public or IaaS domain
     */
    URI authServiceUri(Region region) {
        String realmDomain = region.getRealm()
                .getPublicDomainName()
                .orElseGet(() -> region.getRealm()
                        .getIaasDomainName()
                        .orElseThrow(() -> new IllegalStateException(
                                "Realm domain must be available to derive authentication service URI")));
        return URI.create(String.format(AUTH_SERVICE_URI, region.getPublicRegionName(), realmDomain));
    }

    private Optional<String> stringValue(String key) {
        return Optional.ofNullable(config.get(key).asString().orElse(null))
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
