/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.envconfig.microprofile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.config.MapConfigSource;
import io.helidon.config.mp.MpConfigSources;

import com.oracle.pic.commons.configuration.EnvironmentConfig;
import com.oracle.pic.commons.configuration.location.LocationOverride;
import com.oracle.pic.commons.util.AvailabilityDomain;
import com.oracle.pic.commons.util.Region;
import jakarta.annotation.Priority;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Config Source for common Oci environment properties retrieved from
 * {@link com.oracle.pic.commons.configuration.EnvironmentConfig} getter methods. For example, the iaas domain name value will be
 * stored in the "oci.env.iaasDomainName" config property, and the value is retrieved using EnvironmentConfig.getIaasDomainName().
 */
@Priority(5000)
public class OciEnvMpConfigSource implements ConfigSource {
    static final String ENV_REALM = "realm";
    static final String ENV_REGION = "region";
    static final String ENV_REGION_NAME = "region-name";
    static final String ENV_REGION_INTERNAL_NAME = "region-internal-name";
    static final String ENV_AVAILABILITY_DOMAIN = "availability-domain";
    static final String ENV_FAULT_DOMAIN = "fault-domain";
    static final String ENV_PUBLIC_DOMAIN_NAME = "public-domain-name";
    static final String ENV_REALM_PUBLIC_DOMAIN_NAME = "realm-public-domain-name";
    static final String ENV_OCI_PUBLIC_DOMAIN_NAME = "oci-public-domain-name";
    static final String ENV_IAAS_DOMAIN_NAME = "iaas-domain-name";
    static final String ENV_REALM_IAAS_DOMAIN_NAME = "realm-iaas-domain-name";
    static final String ENV_OCI_IAAS_DOMAIN_NAME = "oci-iaas-domain-name";
    static final String ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME = "first-region-in-realm-public-name";
    static final String ENV_AD_NUMBER = "ad-number";
    static final String ENV_NUMBER_FOR_AD = "number-for-ad";
    static final String ENV_AIRPORT_CODE = "airport-code";
    static final String ENV_DB_TNS_NAME = "db-tns-name";
    static final String ENV_ODO_APPLICATION_ALIAS = "odo-application-alias";
    static final String ENV_ODO_APPLICATION_RESOURCE_ID = "odo-application-resource-id";
    static final String ENV_ODO_POOL_ALIAS = "odo-pool-alias";
    static final String ENV_ODO_POOL_RESOURCE_ID = "odo-pool-resource-id";
    static final String ENV_ODO_BUILD_TAG = "odo-build-tag";

    static final String DEFAULT_PREFIX = "oci.env";

    private static final String DOT = ".";
    private static final int DEFAULT_ORDINAL = 83;
    private static final Logger LOGGER = Logger.getLogger(OciEnvMpConfigSource.class.getName());
    private final int ordinal;
    private ConfigSource configSource;

    /**
     * Default constructor for the ConfigSource when loaded via {@link java.util.ServiceLoader}.
     */
    public OciEnvMpConfigSource() {
        this.ordinal = DEFAULT_ORDINAL;
        configSource = create(Config.empty());
    }

    /**
     * Constructor for the ConfigSource when used with Meta-Config. This will override the ConfigSource created by the default
     * constructor.
     */
    OciEnvMpConfigSource(Config metaConfig, int ordinal) {
        configSource = create(metaConfig);
        this.ordinal = ordinal;
    }

    private ConfigSource create(Config metaConfig) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "OCI env config source meta-config: " + metaConfig);
        }
        LocationOverride locationOverride = null;
        Config locationOverrideConfig = metaConfig.get("location-override");
        if (locationOverrideConfig.exists()) {
            locationOverride = getLocationOverride(locationOverrideConfig);
        }
        boolean usePhysicalAvailabilityDomain =
                metaConfig.get("use-physical-availability-domain").asBoolean().orElse(Boolean.FALSE);

        var ec = new EnvironmentConfig(locationOverride, usePhysicalAvailabilityDomain);
        Map<String, String> envMap = new HashMap<>();
        String prefix = (metaConfig.get("prefix").asString().orElse(DEFAULT_PREFIX));
        if (!prefix.endsWith(DOT)) {
            prefix = prefix + DOT;
        }
        envMap.put(prefix + ENV_REALM, ec.getRealm());
        envMap.put(prefix + ENV_REGION, ec.getRegion());
        envMap.put(prefix + ENV_REGION_NAME, ec.getRegionName());
        envMap.put(prefix + ENV_REGION_INTERNAL_NAME, ec.getRegionInternalName());
        envMap.put(prefix + ENV_AVAILABILITY_DOMAIN, ec.getAvailabilityDomain());
        envMap.put(prefix + ENV_FAULT_DOMAIN, String.valueOf(ec.getFaultDomain()));
        envMap.put(prefix + ENV_PUBLIC_DOMAIN_NAME, ec.getPublicDomainName());
        envMap.put(prefix + ENV_REALM_PUBLIC_DOMAIN_NAME, ec.getRealmPublicDomainName());
        envMap.put(prefix + ENV_OCI_PUBLIC_DOMAIN_NAME, ec.getOciPublicDomainName());
        envMap.put(prefix + ENV_IAAS_DOMAIN_NAME, ec.getIaasDomainName());
        envMap.put(prefix + ENV_REALM_IAAS_DOMAIN_NAME, ec.getRealmIaasDomainName());
        envMap.put(prefix + ENV_OCI_IAAS_DOMAIN_NAME, ec.getOciIaasDomainName());
        envMap.put(prefix + ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME, ec.getFirstRegionInRealmPublicName());
        envMap.put(prefix + ENV_AD_NUMBER, ec.getAdNumber());
        envMap.put(prefix + ENV_NUMBER_FOR_AD, String.valueOf(ec.getNumberForAd()));
        envMap.put(prefix + ENV_AIRPORT_CODE, ec.getAirportCode());
        envMap.put(prefix + ENV_DB_TNS_NAME, ec.getDbTnsName());
        envMap.put(prefix + ENV_ODO_APPLICATION_ALIAS, ec.getOdoApplicationAlias());
        envMap.put(prefix + ENV_ODO_APPLICATION_RESOURCE_ID, ec.getOdoApplicationResourceId());
        envMap.put(prefix + ENV_ODO_POOL_ALIAS, ec.getOdoPoolAlias());
        envMap.put(prefix + ENV_ODO_POOL_RESOURCE_ID, ec.getOdoPoolResourceId());
        envMap.put(prefix + ENV_ODO_BUILD_TAG, ec.getOdoBuildTag());
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "OCI env config source properties: " + envMap);
        }
        return MpConfigSources.create(MapConfigSource.create(envMap));
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getValue(String propertyName) {
        return configSource.getValue(propertyName);
    }

    @Override
    public String getName() {
        return "oci-env";
    }

    private static LocationOverride getLocationOverride(Config config) {
        LocationOverride.LocationOverrideBuilder locationOverrideBuilder = LocationOverride.builder();
        config.get("region").ifExists(c -> locationOverrideBuilder.region(getRegion(c.asString().get())));
        config.get("availability-domain").ifExists(
                c -> locationOverrideBuilder.availabilityDomain(getAvailabilityDomain(c.asString().get())));
        config.get("fault-domain").ifExists(c -> locationOverrideBuilder.faultDomain(c.asInt().get()));
        return locationOverrideBuilder.build();
    }

    // Returns Region from a region string name, e.g. sol-mars-1.
    private static Region getRegion(String name) {
        Region region;
        try {
            region = Region.fromName(name);
            return region;
        } catch (IllegalArgumentException e) {
            try {
                region = Region.fromPublicRegionName(name);
                return region;
            } catch (IllegalArgumentException ex) {
                region = Region.valueOf(name.toUpperCase(Locale.ENGLISH).replace('-', '_'));
                return region;
            }
        }
    }

    // Returns AvailabilityDomain from a string name, eg. sol-mars-1-ad-1
    private static AvailabilityDomain getAvailabilityDomain(String name) {
        AvailabilityDomain availabilityDomain;
        try {
            availabilityDomain = AvailabilityDomain.fromName(name);
            return availabilityDomain;
        } catch (IllegalArgumentException e) {
            availabilityDomain = AvailabilityDomain.valueOf(name.toUpperCase(Locale.ENGLISH).replace('-', '_'));
            return availabilityDomain;
        }
    }
}
