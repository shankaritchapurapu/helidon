/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.Optional;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;
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
    private final java.util.function.Supplier<String> defaultRegionIdSupplier;

    SplatMtlsRequestHandler(SplatMtlsConfig config) {
        this(config, () -> Services.get(com.oracle.bmc.Region.class).getRegionId());
    }

    static SplatMtlsRequestHandler createForTesting(SplatMtlsConfig config,
                                                    java.util.function.Supplier<String> defaultRegionIdSupplier) {
        return new SplatMtlsRequestHandler(config, defaultRegionIdSupplier);
    }

    private SplatMtlsRequestHandler(SplatMtlsConfig config,
                                    java.util.function.Supplier<String> defaultRegionIdSupplier) {
        this.config = config;
        this.defaultRegionIdSupplier = defaultRegionIdSupplier;
    }

    boolean shouldAllow(ServerRequest request,
                        ServerResponse response,
                        ResourceInfo resourceInfo) throws Exception {
        var upstreamFilter = createFilter();
        if (upstreamFilter.isEmpty()) {
            return true;
        }

        return HelidonContainerRequestFilterRunner
                .run(upstreamFilter.orElseThrow(), request, response, resourceInfo)
                .isPresent();
    }

    Optional<ContainerRequestFilter> createFilter() {
        if (!config.enabled()) {
            return Optional.empty();
        }
        return Optional.of(new com.oracle.pic.platform.splat.sdk.mtls.SplatMtlsFilter(resolveRegion(), upstreamConfig()));
    }

    Region resolveRegion() {
        String regionId = config.region().orElseGet(defaultRegionIdSupplier);
        return Region.fromPublicRegionName(regionId);
    }

    com.oracle.pic.platform.splat.sdk.config.SplatMtlsFilterConfig upstreamConfig() {
        var upstreamConfig = new com.oracle.pic.platform.splat.sdk.config.SplatMtlsFilterConfig();
        upstreamConfig.setSkipAuthzValidationCheck(config.skipAuthzValidationCheck());
        upstreamConfig.setRejectXRegionCalls(config.rejectXRegionCalls());
        return upstreamConfig;
    }
}
