/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.pic.commons.util.AvailabilityDomain;
import com.oracle.pic.commons.util.Region;

@Service.Singleton
class OciEnvLocationDefaults {

    private static final System.Logger LOGGER = System.getLogger(OciEnvLocationDefaults.class.getName());
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
        return config.get(OCI_ENV_AVAILABILITY_DOMAIN).asString().asOptional();
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
     * Resolves a physical availability domain using explicit identity configuration first and
     * the resolved {@code oci.env.availability-domain} config value second.
     *
     * @param configuredPhysicalAd explicit identity physical availability domain
     * @param resolvedRegion resolved authorization region used to check the default
     * @return resolved physical availability domain, or empty when neither source provides one
     */
    Optional<String> physicalAd(Optional<String> configuredPhysicalAd, Region resolvedRegion) {
        if (configuredPhysicalAd.isPresent()) {
            return configuredPhysicalAd;
        }
        Optional<String> ociEnvAvailabilityDomain = config.get(OCI_ENV_AVAILABILITY_DOMAIN).asString().asOptional();
        if (ociEnvAvailabilityDomain.isEmpty()) {
            return Optional.empty();
        }

        String defaultPhysicalAd = ociEnvAvailabilityDomain.orElseThrow();
        try {
            AvailabilityDomain availabilityDomain = AvailabilityDomain.fromName(defaultPhysicalAd);
            if (!availabilityDomain.getRegion().equals(resolvedRegion)) {
                // Warn for now, but preserve compatibility by still applying the oci-env default.
                LOGGER.log(System.Logger.Level.WARNING,
                           "Using oci.env.availability-domain {0} as authorization physical AD even though it "
                                   + "belongs to region {1}, not resolved authorization region {2}",
                           defaultPhysicalAd,
                           availabilityDomain.getRegion().getPublicRegionName(),
                           resolvedRegion.getPublicRegionName());
            }
        } catch (RuntimeException e) {
            // If the AD cannot be parsed, keep the previous defaulting behavior and let the Auth SDK handle it.
            LOGGER.log(System.Logger.Level.WARNING,
                       "Using oci.env.availability-domain {0} as authorization physical AD; unable to verify "
                               + "it belongs to resolved authorization region {1}",
                       defaultPhysicalAd,
                       resolvedRegion.getPublicRegionName());
        }
        return ociEnvAvailabilityDomain;
    }

    /**
     * Resolves a required physical availability domain using explicit identity configuration first and
     * {@code oci.env.availability-domain} second.
     *
     * @param configuredPhysicalAd explicit identity physical availability domain
     * @param resolvedRegion resolved authorization region used to check the default
     * @param missingMessage error message to use when neither source provides a physical availability domain
     * @return resolved physical availability domain
     * @throws IllegalStateException when neither source provides a physical availability domain
     */
    String requirePhysicalAd(Optional<String> configuredPhysicalAd, Region resolvedRegion, String missingMessage) {
        return physicalAd(configuredPhysicalAd, resolvedRegion)
                .orElseThrow(() -> new IllegalStateException(missingMessage));
    }

    /**
     * Returns the resolved {@code oci.env.fault-domain} config value.
     *
     * @return fault domain from {@code oci-env}, or empty when none is configured
     */
    Optional<String> faultDomain() {
        return config.get(OCI_ENV_FAULT_DOMAIN).asString().asOptional();
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
}
