/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.lang.System.Logger.Level;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.spi.OciRegion;
import io.helidon.service.registry.Service;

import com.oracle.bmc.Realm;
import com.oracle.bmc.Region;

@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT)
final class OciEnvRegionImpl implements OciRegion {
    private static final System.Logger LOGGER = System.getLogger(OciEnvRegionImpl.class.getName());
    private static final String SYNTHETIC_SECOND_LEVEL_DOMAIN = "oraclecloud.invalid";
    private final LazyValue<Optional<Region>> region;

    @Service.Inject
    OciEnvRegionImpl(Supplier<OciEnvConfigFactory> factory) {
        this.region = LazyValue.create(() -> factory.get().region().flatMap(OciEnvRegionImpl::toSdkRegion));
    }

    @Override
    public Optional<Region> region() {
        return region.get();
    }

    /*
     * Dynamic core-regions import updates the commons-core region catalog, but Helidon's
     * OciRegion contract must still return the OCI SDK Region type. Resolve directly when the
     * SDK already knows the region and lazily bridge commons metadata into the SDK catalog only
     * for the fallback path.
     */
    private static Optional<Region> toSdkRegion(com.oracle.pic.commons.util.Region commonRegion) {
        return toSdkRegion(commonRegion, Region::fromRegionCodeOrId);
    }

    static Optional<Region> toSdkRegion(com.oracle.pic.commons.util.Region commonRegion,
                                        Function<String, Region> sdkRegionLookup) {
        String regionId = commonRegion.getPublicRegionName();
        try {
            Region sdkRegion = sdkRegionLookup.apply(regionId);
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Resolved OCI SDK region ''{0}'' without additional registration", regionId);
            }
            return Optional.of(sdkRegion);
        } catch (IllegalArgumentException e) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "OCI SDK does not know region ''" + regionId + "'', registering it from commons metadata",
                           e);
            }
            return registerSdkRegion(commonRegion);
        }
    }

    /*
     * SDK realm registration needs a second-level domain. Prefer the public domain because that is
     * the normal SDK realm identity, and fall back to the iaas domain for realms that do not have
     * a public domain. If neither exists, synthesize a realm with a reserved invalid domain so
     * Region injection stays aligned with oci-env without routing to a real realm endpoint.
     */
    private static Optional<Region> registerSdkRegion(com.oracle.pic.commons.util.Region commonRegion) {
        Optional<String> secondLevelDomain = commonRegion.getRealm().getPublicDomainName()
                .or(commonRegion.getRealm()::getIaasDomainName);
        String sdkSecondLevelDomain = secondLevelDomain.orElse(SYNTHETIC_SECOND_LEVEL_DOMAIN);
        if (secondLevelDomain.isEmpty() && LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Commons realm ''{0}'' has no public or iaas domain; registering OCI SDK realm with "
                               + "synthetic second-level domain ''{1}'' for region ''{2}''",
                       commonRegion.getRealm().getName(),
                       sdkSecondLevelDomain,
                       commonRegion.getPublicRegionName());
        }

        Realm sdkRealm = Realm.register(commonRegion.getRealm().getName(), sdkSecondLevelDomain);
        Region sdkRegion = Region.register(commonRegion.getPublicRegionName(),
                                           sdkRealm,
                                           normalizedRegionCode(commonRegion.getAirportCode()));
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Registered OCI SDK region ''{0}'' in realm ''{1}'' using commons metadata",
                       sdkRegion.getRegionId(),
                       sdkRealm.getRealmId());
        }
        return Optional.of(sdkRegion);
    }

    private static String normalizedRegionCode(String regionCode) {
        if (regionCode == null) {
            return null;
        }
        String normalized = regionCode.trim().toLowerCase(Locale.US);
        return normalized.isEmpty() ? null : normalized;
    }
}
