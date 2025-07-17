/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.http.PathMatcher;

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JakartaContainerRequestFilter;
import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxContainerRequestContext;
import com.oracle.pic.identity.authorization.sdk.AuthorizationClient;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;

import static com.oracle.helidon.oci.identity.IdentityPathMatchingRequestFilter.loadPathMatchers;

/**
 * For back-end services (i.e., Compute, Block Store) that does not perform authentication. Rather it depends on the
 * front-end proxy api to send back principal object in a header.
 */
@Priority(Priorities.AUTHENTICATION)
public class BackendServiceAuthContextRequestFilter
        extends com.oracle.pic.identity.authorization.sdk.BackendServiceAuthContextRequestFilter
        implements IdentityPathMatchingRequestFilter {

    /**
     * Configuration name of this filter.
     */
    public static final String NAME = "backend-service-auth-context";
    private static final LazyValue<List<PathMatcher>> MATCHERS =
            LazyValue.create(() -> loadPathMatchers(NAME));

    /**
     * Constructor that enforces authorization by default and fails the request if authorization fails.
     * @param authorizationClient Authorization client
     */
    @Inject
    public BackendServiceAuthContextRequestFilter(AuthorizationClient authorizationClient) {
        super(authorizationClient);
    }

    @Override
    public Supplier<List<PathMatcher>> pathMatchers() {
        return MATCHERS;
    }

    @Override
    public void actualFilter(ContainerRequestContext ctx) throws IOException {
        JakartaContainerRequestFilter.translateExceptions(() -> super.filter(new JavaxContainerRequestContext(ctx)));
    }
}
