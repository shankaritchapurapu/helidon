/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import io.helidon.common.uri.UriInfo;
import io.helidon.common.uri.UriPath;
import io.helidon.config.mp.MpConfig;
import io.helidon.http.PathMatcher;
import io.helidon.http.PathMatchers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.eclipse.microprofile.config.ConfigProvider;

interface IdentityPathMatchingRequestFilter extends ContainerRequestFilter {

    static List<PathMatcher> loadPathMatchers(String name) {
        return MpConfig
                .toHelidonConfig(ConfigProvider.getConfig())
                .get("oci.identity.filters." + name + ".paths.path")
                .asList(String.class)
                .stream()
                .flatMap(Collection::stream)
                .map(PathMatchers::create)
                .toList();
    }

    void actualFilter(ContainerRequestContext requestContext) throws IOException;

    Supplier<List<PathMatcher>> pathMatchers();

    @SuppressWarnings("unchecked")
    default void filter(ContainerRequestContext ctx) throws IOException {
        Supplier<UriInfo> uriInfoSupplier = (Supplier<UriInfo>) ctx.getProperty(UriInfo.class.getName());
        UriPath uriPath = uriInfoSupplier.get().path();
        if (pathMatchers().get().stream().anyMatch(pathMatcher -> pathMatcher.prefixMatch(uriPath).accepted())) {
            actualFilter(ctx);
        }
    }
}
