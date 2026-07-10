/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Locale;
import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.config.SplatAwareAuthConfig;

/**
 * Factory for the Auth SDK request filter used by generated OCI authorization interceptors.
 */
@Service.Singleton
public class AuthContextRequestFilterFactory {

    private final Config config;

    AuthContextRequestFilterFactory(Config config) {
        this.config = config;
    }

    /**
     * Create the SPLAT-aware Auth SDK request filter using registry-provided Auth clients.
     *
     * @return Auth SDK request filter
     */
    public AuthContextRequestFilter create() {
        return create(Services.get(AuthenticatorClient.class), Services.first(IAuthorizationClient.class));
    }

    /**
     * Create the SPLAT-aware Auth SDK request filter using only authentication.
     *
     * @return Auth SDK request filter without an authorization client
     */
    public AuthContextRequestFilter createAuthenticatedOnly() {
        return create(Services.get(AuthenticatorClient.class), Optional.empty());
    }

    /**
     * Create the SPLAT-aware Auth SDK request filter.
     *
     * @param authenticatorClient authenticator client
     * @param authorizationClient optional authorization client
     * @return Auth SDK request filter
     */
    public AuthContextRequestFilter create(AuthenticatorClient authenticatorClient,
                                           Optional<IAuthorizationClient> authorizationClient) {
        return new ProvenanceAwareSplatAuthContextRequestFilter(
                authenticatorClient,
                authorizationClient.orElse(null),
                splatAwareAuthConfig(),
                resolveRegion(authorizationClient));
    }

    private SplatAwareAuthConfig splatAwareAuthConfig() {
        SplatAwareConfig splatAware = splatAwareConfig();
        SplatAwareAuthConfig authConfig = new SplatAwareAuthConfig();
        authConfig.setSplatRequestPort(splatAware.splatRequestPort());
        authConfig.setAdditionalSplatRequestPorts(splatAware.additionalSplatRequestPorts());
        authConfig.setSkipAuthorizationForSplat(splatAware.skipAuthorizationForSplat());
        authConfig.setValidateSplatCert(splatAware.validateSplatCert());
        authConfig.setDisableTagOnlyRequestCheck(splatAware.disableTagOnlyRequestCheck());
        authConfig.setRejectXRegionCalls(splatAware.rejectXRegionCalls());
        return authConfig;
    }

    private Region resolveRegion(Optional<IAuthorizationClient> authorizationClient) {
        return configValue("oci.identity.splat-aware.region")
                .or(() -> configValue("oci.identity.authentication.region"))
                .or(() -> configValue("oci.identity.authorization.region"))
                .or(() -> authorizationClient.map(IAuthorizationClient::getRegion))
                .filter(region -> !region.isBlank())
                .map(AuthContextRequestFilterFactory::toRegion)
                .or(() -> Services.first(Region.class))
                .orElseThrow(() -> new IllegalStateException(
                        "A region must be configured for SPLAT-aware identity filtering. Configure one of "
                                + "oci.identity.splat-aware.region, oci.identity.authentication.region, "
                                + "oci.identity.authorization.region, provide a region-bearing authorization client, "
                                + "or provide Region in the service registry."));
    }

    private SplatAwareConfig splatAwareConfig() {
        Config splatAware = config.get("oci.identity.splat-aware");
        if (splatAware.type() == Config.Type.MISSING) {
            return SplatAwareConfig.create();
        }
        return SplatAwareConfig.create(splatAware);
    }

    private Optional<String> configValue(String key) {
        return Optional.ofNullable(config.get(key).asString().orElse(null))
                .filter(value -> !value.isBlank());
    }

    private static Region toRegion(String region) {
        String name = region.trim();
        try {
            return Region.fromPublicRegionName(name);
        } catch (RuntimeException e) {
            String airportCode = name.toUpperCase(Locale.ROOT);
            if (Region.isAirportCode(airportCode)) {
                return Region.fromAirportCode(airportCode);
            }
            return Region.fromInternalName(name);
        }
    }
}
