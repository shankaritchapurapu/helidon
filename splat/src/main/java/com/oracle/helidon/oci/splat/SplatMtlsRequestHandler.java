/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;

import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.oracle.helidon.oci.jaxrs.HelidonContainerRequestFilterRunner;
import com.oracle.pic.commons.util.Region;

/**
 * Creates and runs the upstream SPLAT JAX-RS filter against Helidon requests.
 */
@Service.Singleton
class SplatMtlsRequestHandler {
    private final SplatMtlsConfig config;
    private final Supplier<Region> defaultRegionSupplier;
    private final SplatMtlsRequestValidator requestValidator;

    @Service.Inject
    SplatMtlsRequestHandler(SplatMtlsConfig config,
                            Supplier<Region> defaultRegionSupplier) {
        this.config = config;
        this.defaultRegionSupplier = Objects.requireNonNull(defaultRegionSupplier);
        this.requestValidator = new SplatMtlsRequestValidator(config);
    }

    boolean shouldAllow(ServerRequest request,
                        ServerResponse response,
                        ResourceInfo resourceInfo) throws Exception {
        if (!config.enabled()) {
            return true;
        }

        Region region = resolveRegion();
        var validationFailure = requestValidator.validate(request, region);
        if (validationFailure.isPresent()) {
            response.status(403).send(validationFailure.orElseThrow());
            return false;
        }

        return HelidonContainerRequestFilterRunner
                .run(createFilter(region), request, response, resourceInfo)
                .isPresent();
    }

    Optional<ContainerRequestFilter> createFilter() {
        if (!config.enabled()) {
            return Optional.empty();
        }
        return Optional.of(createFilter(resolveRegion()));
    }

    private ContainerRequestFilter createFilter(Region region) {
        return new com.oracle.pic.platform.splat.sdk.mtls.SplatMtlsFilter(region, upstreamConfig());
    }

    Region resolveRegion() {
        return config.region()
                .map(Region::fromPublicRegionName)
                .orElseGet(defaultRegionSupplier);
    }

    com.oracle.pic.platform.splat.sdk.config.SplatMtlsFilterConfig upstreamConfig() {
        var upstreamConfig = new com.oracle.pic.platform.splat.sdk.config.SplatMtlsFilterConfig();
        upstreamConfig.setSkipAuthzValidationCheck(config.skipAuthzValidationCheck());
        upstreamConfig.setRejectXRegionCalls(config.rejectXRegionCalls());
        return upstreamConfig;
    }
}
