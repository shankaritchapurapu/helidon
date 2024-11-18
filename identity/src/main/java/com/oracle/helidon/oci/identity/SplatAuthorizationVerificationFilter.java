/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.uri.UriInfo;
import io.helidon.common.uri.UriPath;
import io.helidon.http.PathMatcher;

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JakartaContainerRequestFilter;
import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxContainerRequestContext;
import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxContainerResponseContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

import static com.oracle.helidon.oci.identity.IdentityPathMatchingRequestFilter.loadPathMatchers;

/**
 * This filter is to verify Authorization requests when authorization automation is set to 'verification' mode in Splat.
 *
 * Splat sets a request header which contains the set of AuthorizationRequest hash for all resources associated
 * with an operation. This filter gets the hash from Request header and compares with hash of authorizationRequests
 * created by AuthorizationClient for a request.
 *
 */
public class SplatAuthorizationVerificationFilter
        extends com.oracle.pic.identity.authorization.sdk.SplatAuthorizationVerificationFilter
        implements ContainerResponseFilter {

    /**
     * Configuration name of this filter.
     */
    public static final String NAME = "splat-authorization-verification";
    private static final LazyValue<List<PathMatcher>> MATCHERS =
            LazyValue.create(() -> loadPathMatchers(NAME));

    @Override
    @SuppressWarnings("unchecked")
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext resCtx) throws IOException {
        Supplier<UriInfo> uriInfoSupplier = (Supplier<UriInfo>) reqCtx.getProperty(UriInfo.class.getName());
        UriPath uriPath = uriInfoSupplier.get().path();
        if (MATCHERS.get().stream().anyMatch(pathMatcher -> pathMatcher.prefixMatch(uriPath).accepted())) {
            JavaxContainerRequestContext javaxReqCtx = new JavaxContainerRequestContext(reqCtx);
            JavaxContainerResponseContext javaxResCtx = new JavaxContainerResponseContext(resCtx);
            JakartaContainerRequestFilter.translateExceptions(() -> super.filter(javaxReqCtx, javaxResCtx));
        }
    }
}
