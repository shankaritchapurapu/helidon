/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.common.mapper.OptionalValue;
import io.helidon.common.mapper.Value;
import io.helidon.common.uri.UriInfo;
import io.helidon.common.uri.UriPath;
import io.helidon.config.Config;
import io.helidon.config.mp.MpConfig;
import io.helidon.http.PathMatcher;
import io.helidon.http.PathMatchers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.eclipse.microprofile.config.ConfigProvider;

import static java.util.function.Predicate.not;

interface IdentityPathMatchingRequestFilter extends ContainerRequestFilter {

    static List<PathMatcher> loadPathMatchers(String name) {
        return loadPathMatchers(MpConfig.toHelidonConfig(ConfigProvider.getConfig()), name);
    }

    static List<PathMatcher> loadPathMatchers(Config config, String name) {

        // oci.identity:
        //  filters:
        //    auth-context:
        //      paths:
        //      - path: /v1/cars
        //      - path: /v1/bikes
        //      - path: /v1/trains/*
        Stream<String> newStyle = config.get("oci.identity.filters." + name + ".paths")
                .asList(c -> c.get("path").asString())
                .stream()
                .flatMap(Collection::stream)
                .filter(not(OptionalValue::isEmpty))
                .flatMap(Value::stream);


        // Backward compatibility with HLDN-200 workaround
        // oci.identity:
        //  filters:
        //    auth-context:
        //      paths.path:
        //      - /v1/cars
        //      - /v1/bikes
        //      - /v1/trains/*
        Stream<String> oldStyle = config.get("oci.identity.filters." + name + ".paths.path")
                .asList(String.class)
                .stream()
                .flatMap(Collection::stream);

        return Stream.concat(newStyle, oldStyle)
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
