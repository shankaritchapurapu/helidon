/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.http.PathMatcher;
import io.helidon.service.registry.GlobalServiceRegistry;

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JakartaContainerRequestFilter;
import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxContainerRequestContext;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authorization.sdk.AuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.config.SplatAwareAuthConfig;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;

import static com.oracle.helidon.oci.identity.IdentityPathMatchingRequestFilter.loadPathMatchers;

/**
 * Skips authentication if a request is identified as a splat request. It will also mark splat requests to skip AuthZ
 * based on configured values and request headers.
 * Note: It is required the port specified for Splat requests is secured with required configuration
 * e.g mTLS endpoint.
 */
@Priority(Priorities.AUTHENTICATION)
public class SplatAwareAuthContextRequestFilter
        extends com.oracle.pic.identity.authorization.sdk.SplatAwareAuthContextRequestFilter
        implements IdentityPathMatchingRequestFilter {

    /**
     * Configuration name of this filter.
     */
    public static final String NAME = "splat-aware-auth-context";
    private static final LazyValue<List<PathMatcher>> MATCHERS =
            LazyValue.create(() -> loadPathMatchers(NAME));

    /**
     * Default constructor for client/config injection.
     *
     * @param authenticatorClient
     * @param authorizationClient
     * @param splatAwareAuthConfig
     */
    @Inject
    public SplatAwareAuthContextRequestFilter(jakarta.inject.Provider<AuthenticatorClient> authenticatorClient,
                                              jakarta.inject.Provider<AuthorizationClient> authorizationClient,
                                              jakarta.inject.Provider<SplatAwareAuthConfig> splatAwareAuthConfig) {
        super(authenticatorClient.get(),
              authorizationClient.get(),
              splatAwareAuthConfig.get(),
              Region.fromPublicRegionName(GlobalServiceRegistry.registry()
                                                  .get(com.oracle.bmc.Region.class)
                                                  .getRegionId()));
    }

    @Override
    public void actualFilter(ContainerRequestContext ctx) throws IOException {
        JakartaContainerRequestFilter.translateExceptions(() -> super.filter(new JavaxContainerRequestContext(ctx)));
    }

    @Override
    public Supplier<List<PathMatcher>> pathMatchers() {
        return MATCHERS;
    }
}
