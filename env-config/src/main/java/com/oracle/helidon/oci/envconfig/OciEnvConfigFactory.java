/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.config.ConfigSources;
import io.helidon.config.MetaConfig;
import io.helidon.config.spi.ConfigNode;
import io.helidon.integrations.oci.ImdsInstanceInfo;
import io.helidon.service.registry.Service;

import com.oracle.pic.commons.configuration.EnvironmentConfig;
import com.oracle.pic.commons.configuration.location.LocationOverride;
import com.oracle.pic.commons.util.AvailabilityDomain;
import com.oracle.pic.commons.util.Realm;
import com.oracle.pic.commons.util.Region;

@Service.Singleton
class OciEnvConfigFactory {
    static final String DEFAULT_PREFIX = "oci.env";
    static final Path DEFAULT_REGION_PATH = Path.of("/etc/region");
    static final Path DEFAULT_AVAILABILITY_DOMAIN_PATH = Path.of("/etc/availability-domain");
    static final Path DEFAULT_PHYSICAL_AVAILABILITY_DOMAIN_PATH = Path.of("/etc/physical-availability-domain");
    static final Path DEFAULT_FAULT_DOMAIN_PATH = Path.of("/etc/fault-domain");

    private static final System.Logger LOGGER = System.getLogger(OciEnvConfigFactory.class.getName());
    private static final Set<Realm> NOT_SUPPORTED_PUBLIC_REALMS = Set.of(Realm.DEV, Realm.INTEG_STABLE, Realm.INTEG_NEXT);
    private static final String OCI_CONFIG_PATH = "oci-config.yaml";
    private static final String OCI_CONFIG_PREFIX = "helidon.oci-env";

    private final OciEnvConfig config;
    private final OciEnvLocationOverride locationOverride;
    private final DynamicCoreRegions dynamicCoreRegions;
    private final LazyValue<Optional<ImdsInstanceInfo>> imdsInstanceInfo;

    @Service.Inject
    OciEnvConfigFactory(@Service.Named(OciEnvConfigSourceProvider.TYPE) Optional<MetaConfig> metaConfig,
                        Supplier<Optional<ImdsInstanceInfo>> imdsInstanceInfo) {
        this(serviceConfig(metaConfig),
             imdsInstanceInfo);
    }

    OciEnvConfigFactory(Config config) {
        this(config, Optional::empty);
    }

    OciEnvConfigFactory(Optional<MetaConfig> metaConfig) {
        this(metaConfig.map(MetaConfig::metaConfiguration).orElseGet(OciEnvConfigFactory::ociConfig));
    }

    OciEnvConfigFactory(Config config, Supplier<Optional<ImdsInstanceInfo>> imdsInstanceInfo) {
        this.config = OciEnvConfig.create(config);
        this.locationOverride = this.config.locationOverride().orElseGet(OciEnvLocationOverride::create);
        this.dynamicCoreRegions = new DynamicCoreRegions(this.config.dynamicCoreRegions()
                                                                 .orElseGet(OciEnvDynamicCoreRegions::create));
        this.imdsInstanceInfo = LazyValue.create(() -> {
            Optional<ImdsInstanceInfo> value = Objects.requireNonNull(imdsInstanceInfo, "imdsInstanceInfo").get();
            return value == null ? Optional.empty() : value;
        });
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Initialized oci-env factory with prefix ''{0}'' and dev override {1}",
                       this.config.prefix(),
                       this.config.locationOverrideDev());
        }
    }

    String prefix() {
        return config.prefix();
    }

    ConfigNode.ObjectNode create() {
        return createRoot();
    }

    ConfigNodes createNodes() {
        return configNodes(createRoot());
    }

    Optional<Region> region() {
        dynamicCoreRegions.importIfConfigured();
        return resolveRegionIfAvailable(false);
    }

    Optional<String> regionId() {
        return region()
                .map(Region::getPublicRegionName);
    }

    static ConfigNodes configNodes(ConfigNode.ObjectNode root) {
        return new ConfigNodes(root, indexNodes(root));
    }

    private ConfigNode.ObjectNode createRoot() {
        dynamicCoreRegions.importIfConfigured();

        EnvironmentConfig envCfg = new EnvironmentConfig(resolveLocationOverride(), false);

        String prefix = config.prefix();
        String prefixedKey = prefix + '.';
        ConfigNode.ObjectNode.Builder values = ConfigNode.ObjectNode.builder();

        addValue(values, prefixedKey + "realm", envCfg.getRealm());
        addValue(values, prefixedKey + "region", envCfg.getRegion());
        addValue(values, prefixedKey + "region-name", envCfg.getRegionName());
        addValue(values, prefixedKey + "region-internal-name", envCfg.getRegionInternalName());
        addValue(values, prefixedKey + "availability-domain", envCfg.getAvailabilityDomain());
        addValue(values, prefixedKey + "fault-domain", envCfg.getFaultDomain());

        addValue(values, prefixedKey + "public-domain-name", envCfg.getPublicDomainName());
        addValue(values, prefixedKey + "realm-public-domain-name", envCfg.getRealmPublicDomainName());
        addValue(values, prefixedKey + "oci-public-domain-name", envCfg.getOciPublicDomainName());
        addValue(values, prefixedKey + "iaas-domain-name", envCfg.getIaasDomainName());
        addValue(values, prefixedKey + "realm-iaas-domain-name", envCfg.getRealmIaasDomainName());
        addValue(values, prefixedKey + "oci-iaas-domain-name", envCfg.getOciIaasDomainName());
        addValue(values, prefixedKey + "first-region-in-realm-public-name", envCfg.getFirstRegionInRealmPublicName());

        addValue(values, prefixedKey + "ad-number", envCfg.getAdNumber());
        addValue(values, prefixedKey + "number-for-ad", envCfg.getNumberForAd());
        addValue(values, prefixedKey + "airport-code", envCfg.getAirportCode());
        addValue(values, prefixedKey + "db-tns-name", envCfg.getDbTnsName());

        addValue(values, prefixedKey + "odo-application-alias", envCfg.getOdoApplicationAlias());
        addValue(values, prefixedKey + "odo-application-resource-id", envCfg.getOdoApplicationResourceId());
        addValue(values, prefixedKey + "odo-pool-alias", envCfg.getOdoPoolAlias());
        addValue(values, prefixedKey + "odo-pool-resource-id", envCfg.getOdoPoolResourceId());
        addValue(values, prefixedKey + "odo-build-tag", envCfg.getOdoBuildTag());

        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Published oci-env values for prefix ''{0}'' using region ''{1}'' and availability domain ''{2}''",
                       prefix,
                       envCfg.getRegion(),
                       envCfg.getAvailabilityDomain());
        }
        return values.build();
    }

    private static void addValue(ConfigNode.ObjectNode.Builder values, String key, Object value) {
        Optional.ofNullable(value)
                .map(String::valueOf)
                .ifPresent(stringValue -> values.addValue(key, stringValue));
    }

    private static Map<String, ConfigNode> indexNodes(ConfigNode.ObjectNode root) {
        Map<String, ConfigNode> nodesByKey = new HashMap<>();
        root.forEach((key, node) -> indexNode(key, node, nodesByKey));
        return Map.copyOf(nodesByKey);
    }

    private static void indexNode(String key, ConfigNode node, Map<String, ConfigNode> nodesByKey) {
        nodesByKey.put(key, node);
        if (node instanceof ConfigNode.ObjectNode objectNode) {
            objectNode.forEach((childKey, childNode) ->
                                       indexNode(key + "." + childKey, childNode, nodesByKey));
        }
    }

    private LocationOverride resolveLocationOverride() {
        if (config.locationOverrideDev()) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Using DEV location override for oci-env resolution");
            }
            return devLocationOverride();
        }

        Region region = resolveRegion(true);
        AvailabilityDomain availabilityDomain = resolveAvailabilityDomain(region);
        validateLocation(region, availabilityDomain);
        Optional<Integer> faultDomain = resolveFaultDomain();

        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Resolved oci-env location to region ''{0}'', availability domain ''{1}'', and fault domain ''{2}''",
                       region.getPublicRegionName(),
                       availabilityDomain.getName(),
                       faultDomain.map(String::valueOf).orElse("absent"));
        }

        return LocationOverride.builder()
                .region(region)
                .availabilityDomain(availabilityDomain)
                .faultDomain(faultDomain.orElse(null))
                .build();
    }

    private static LocationOverride devLocationOverride() {
        return LocationOverride.builder()
                .region(Region.DEV)
                .availabilityDomain(AvailabilityDomain.DEV_1)
                .build();
    }

    private Region resolveRegion(boolean useImdsFallback) {
        return resolveRegionIfAvailable(useImdsFallback)
                .orElseThrow(() -> new ConfigException(
                        useImdsFallback
                                ? String.format("Could not look up Region from local file (%s) or IMDS", DEFAULT_REGION_PATH)
                                : String.format("Could not look up Region from local file (%s)", DEFAULT_REGION_PATH)));
    }

    private Optional<Region> resolveRegionIfAvailable(boolean useImdsFallback) {
        if (config.locationOverrideDev()) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Using DEV region override");
            }
            return Optional.of(Region.DEV);
        }

        Optional<String> regionOverride = locationOverride.region();
        if (regionOverride.isPresent()) {
            Region region = reverseRegionLookup().get(regionOverride.get().toLowerCase(Locale.ENGLISH));
            if (region == null) {
                throw new ConfigException(String.format("Configured location override region '%s' is invalid",
                                                       regionOverride.get()));
            }
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Resolved region from configured override ''{0}'' to ''{1}''",
                           regionOverride.get(),
                           region.getPublicRegionName());
            }
            return Optional.of(region);
        }

        Optional<String> fileRegion = readFileValue(DEFAULT_REGION_PATH);
        if (fileRegion.isPresent()) {
            return fileRegion
                    .map(regionName -> Region.optionalFromInternalName(regionName)
                            .orElseThrow(() -> new ConfigException(
                                    String.format("Could not look up Region for region '%s' found in local file (%s)",
                                                  regionName,
                                                  DEFAULT_REGION_PATH))))
                    .map(region -> {
                        if (LOGGER.isLoggable(Level.DEBUG)) {
                            LOGGER.log(Level.DEBUG,
                                       "Resolved region from file ''{0}'' to ''{1}''",
                                       DEFAULT_REGION_PATH,
                                       region.getPublicRegionName());
                        }
                        return region;
                    });
        }

        if (!useImdsFallback) {
            return Optional.empty();
        }

        return resolveRegionFromImds();
    }

    private AvailabilityDomain resolveAvailabilityDomain(Region region) {
        Optional<String> availabilityDomainOverride = locationOverride.availabilityDomain();
        if (availabilityDomainOverride.isPresent()) {
            AvailabilityDomain availabilityDomain =
                    reverseAvailabilityDomainLookup().get(availabilityDomainOverride.get().toLowerCase(Locale.ENGLISH));
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Resolved availability domain from configured override ''{0}'' to ''{1}''",
                           availabilityDomainOverride.get(),
                           availabilityDomain == null ? "null" : availabilityDomain.getName());
            }
            return availabilityDomain;
        }

        Path availabilityDomainPath = config.usePhysicalAvailabilityDomain()
                ? DEFAULT_PHYSICAL_AVAILABILITY_DOMAIN_PATH
                : DEFAULT_AVAILABILITY_DOMAIN_PATH;
        Optional<String> fileAvailabilityDomain = readFileValue(availabilityDomainPath);
        if (fileAvailabilityDomain.isPresent()) {
            String availabilityDomainName = fileAvailabilityDomain.get();
            try {
                AvailabilityDomain availabilityDomain =
                        AvailabilityDomain.fromRegionAndAdNumberName(region, availabilityDomainName);
                if (LOGGER.isLoggable(Level.DEBUG)) {
                    LOGGER.log(Level.DEBUG,
                               "Resolved availability domain from file ''{0}'' to ''{1}''",
                               availabilityDomainPath,
                               availabilityDomain.getName());
                }
                return availabilityDomain;
            } catch (RuntimeException e) {
                throw new ConfigException(
                        String.format("Found invalid availability domain from region %s and ad number %s from "
                                              + "local files (%s, %s)",
                                      region.getInternalName(),
                                      availabilityDomainName,
                                      DEFAULT_REGION_PATH,
                                      availabilityDomainPath),
                        e);
            }
        }

        return resolveAvailabilityDomainFromImds(region)
                .orElseThrow(() -> new ConfigException(String.format("Could not look up AD from local file (%s) or IMDS",
                                                                     availabilityDomainPath)));
    }

    private Optional<Integer> resolveFaultDomain() {
        Optional<Integer> faultDomain = locationOverride.faultDomain()
                .or(() -> readFileValue(DEFAULT_FAULT_DOMAIN_PATH)
                        .flatMap(OciEnvConfigFactory::parseFaultDomain))
                .or(this::resolveFaultDomainFromImds);
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Resolved fault domain to ''{0}''",
                       faultDomain.map(String::valueOf).orElse("absent"));
        }
        return faultDomain;
    }

    private void validateLocation(Region region, AvailabilityDomain availabilityDomain) {
        if (region == null) {
            throw new ConfigException(String.format("Configured location override region '%s' is invalid",
                                                   locationOverride.region().orElse("null")));
        }
        if (availabilityDomain == null) {
            throw new ConfigException(String.format("Configured location override availability-domain '%s' is invalid",
                                                   locationOverride.availabilityDomain().orElse("null")));
        }
        if (!availabilityDomain.getRegion().equals(region)) {
            throw new ConfigException(String.format("Configured region '%s' does not match availability domain '%s'",
                                                   region.getPublicRegionName(),
                                                   availabilityDomain.getName()));
        }
    }

    private static Optional<Integer> parseFaultDomain(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            int separator = trimmed.lastIndexOf('-');
            if (separator < 0 || separator == trimmed.length() - 1) {
                return Optional.empty();
            }
            try {
                return Optional.of(Integer.parseInt(trimmed.substring(separator + 1)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
    }

    private Optional<Region> resolveRegionFromImds() {
        Optional<ImdsInstanceInfo> instanceInfo = imdsInstanceInfo.get();
        if (instanceInfo.isEmpty()) {
            return Optional.empty();
        }

        ImdsInstanceInfo metadata = instanceInfo.get();
        Optional<Region> canonicalRegion = regionFromImdsValue(metadata.canonicalRegionName());
        if (canonicalRegion.isPresent()) {
            logImdsRegion(canonicalRegion.get());
            return canonicalRegion;
        }
        Optional<Region> region = regionFromImdsValue(metadata.region());
        if (region.isPresent()) {
            logImdsRegion(region.get());
            return region;
        }
        throw new ConfigException(String.format(
                "Could not look up Region from IMDS metadata canonicalRegionName '%s' or region '%s'",
                metadata.canonicalRegionName(),
                metadata.region()));
    }

    private static void logImdsRegion(Region region) {
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Resolved region from IMDS to ''{0}''",
                       region.getPublicRegionName());
        }
    }

    private Optional<Region> regionFromImdsValue(String value) {
        Optional<String> candidate = nonBlank(value);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(reverseRegionLookup().get(candidate.get().toLowerCase(Locale.ENGLISH)));
    }

    private Optional<AvailabilityDomain> resolveAvailabilityDomainFromImds(Region region) {
        Optional<ImdsInstanceInfo> instanceInfo = imdsInstanceInfo.get();
        if (instanceInfo.isEmpty()) {
            return Optional.empty();
        }

        String imdsAvailabilityDomain = instanceInfo.get().ociAdName();
        Optional<AvailabilityDomain> availabilityDomain = availabilityDomainFromImdsValue(region, imdsAvailabilityDomain);
        if (availabilityDomain.isEmpty() && nonBlank(imdsAvailabilityDomain).isPresent()) {
            throw new ConfigException(String.format(
                    "Could not look up AvailabilityDomain from IMDS metadata ociAdName '%s' for region '%s'",
                    imdsAvailabilityDomain,
                    region.getPublicRegionName()));
        }
        availabilityDomain.ifPresent(ad -> {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Resolved availability domain from IMDS to ''{0}''",
                           ad.getName());
            }
        });
        return availabilityDomain;
    }

    private Optional<AvailabilityDomain> availabilityDomainFromImdsValue(Region region, String value) {
        Optional<String> candidate = nonBlank(value);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        String normalized = candidate.get().toLowerCase(Locale.ENGLISH);
        AvailabilityDomain availabilityDomain = reverseAvailabilityDomainLookup().get(normalized);
        if (availabilityDomain != null) {
            return Optional.of(availabilityDomain);
        }

        try {
            return Optional.of(AvailabilityDomain.fromRegionAndAdNumberName(region, normalized));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private Optional<Integer> resolveFaultDomainFromImds() {
        return imdsInstanceInfo.get()
                .map(ImdsInstanceInfo::faultDomain)
                .flatMap(OciEnvConfigFactory::parseFaultDomain);
    }

    private static Optional<String> nonBlank(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(candidate -> !candidate.isEmpty());
    }

    private Map<String, Region> reverseRegionLookup() {
        Map<String, Region> lookup = new HashMap<>();
        for (Region region : Region.values()) {
            lookup.put(region.getName().toLowerCase(Locale.ENGLISH), region);
            lookup.put(region.name().toLowerCase(Locale.ENGLISH), region);
            if (!NOT_SUPPORTED_PUBLIC_REALMS.contains(region.getRealm())) {
                lookup.put(region.getInternalName().toLowerCase(Locale.ENGLISH), region);
                lookup.put(region.getPublicRegionName().toLowerCase(Locale.ENGLISH), region);
            }
        }
        return lookup;
    }

    private Map<String, AvailabilityDomain> reverseAvailabilityDomainLookup() {
        Map<String, AvailabilityDomain> lookup = new HashMap<>();
        for (AvailabilityDomain availabilityDomain : AvailabilityDomain.values()) {
            lookup.put(availabilityDomain.getName().toLowerCase(Locale.ENGLISH), availabilityDomain);
            lookup.put(availabilityDomain.name().toLowerCase(Locale.ENGLISH), availabilityDomain);
        }
        return lookup;
    }

    Optional<String> readFileValue(Path path) {
        try {
            if (!Files.exists(path)) {
                if (LOGGER.isLoggable(Level.DEBUG)) {
                    LOGGER.log(Level.DEBUG, "oci-env runtime file ''{0}'' does not exist", path);
                }
                return Optional.empty();
            }
            String value = Files.readString(path).trim();
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Read oci-env runtime file ''{0}'' with {1}",
                           path,
                           value.isEmpty() ? "an empty value" : "a non-empty value");
            }
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Config providerConfig(Config metaConfig) {
        Objects.requireNonNull(metaConfig);
        // Provider/meta-config properties are first so they win per key; oci-config.yaml fills gaps.
        return Config.just(ConfigSources.create(metaConfig),
                           ConfigSources.create(ociConfig()));
    }

    static Config serviceConfig(Optional<MetaConfig> metaConfig) {
        return metaConfig.map(MetaConfig::metaConfiguration)
                .map(OciEnvConfigFactory::providerConfig)
                .orElseGet(OciEnvConfigFactory::ociConfig);
    }

    private static Config ociConfig() {
        Config config = Config.create(ConfigSources.file(OCI_CONFIG_PATH).optional(true),
                                      ConfigSources.classpath(OCI_CONFIG_PATH).optional(true));
        Config ociEnvConfig = config.get(OCI_CONFIG_PREFIX);
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       ociEnvConfig.exists()
                               ? "Loaded oci-env configuration from ''{0}'' subtree ''{1}''"
                               : "No ''{1}'' subtree found in ''{0}'', using default oci-env behavior",
                       OCI_CONFIG_PATH,
                       OCI_CONFIG_PREFIX);
        }
        return ociEnvConfig.exists() ? ociEnvConfig : Config.empty();
    }

    record ConfigNodes(ConfigNode.ObjectNode root,
                       Map<String, ConfigNode> nodesByKey) {
        ConfigNodes {
            Objects.requireNonNull(root);
            Objects.requireNonNull(nodesByKey);
        }
    }
}
