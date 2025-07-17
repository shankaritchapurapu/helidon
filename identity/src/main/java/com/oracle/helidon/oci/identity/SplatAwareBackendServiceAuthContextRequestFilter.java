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
import com.oracle.pic.identity.authorization.sdk.AuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.config.SplatAwareAuthConfig;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;

import static com.oracle.helidon.oci.identity.IdentityPathMatchingRequestFilter.loadPathMatchers;

/**
 * An extension of the {@linkplain SplatAwareAuthContextRequestFilter} class for back-end services (i.e., Compute, Block Store) that does not perform authentication.
 * Rather it depends on the AuthProxy to send back principal object in a header.
 * AuthProxy also sends callers certificate CommonName(CN) in "X-Oracle-Auth-Client-CN" header if client call AuthProxy with mTLS connection.
 *
 *
 * The filter identify if a request is Splat Request by certificate CN. If the certificate CN starts with "splat-", it is treated as splat request.
 *
 * Note: SplatAwareAuthConfig.splatRequestPort configuration property is not applicable in this filter.
 */
@Priority(Priorities.AUTHENTICATION)
public class SplatAwareBackendServiceAuthContextRequestFilter
        extends com.oracle.pic.identity.authorization.sdk.SplatAwareBackendServiceAuthContextRequestFilter
        implements IdentityPathMatchingRequestFilter {

    /**
     * Configuration name of this filter.
     */
    public static final String NAME = "splat-aware-backend-service-auth-context";
    private static final LazyValue<List<PathMatcher>> MATCHERS =
            LazyValue.create(() -> loadPathMatchers(NAME));

    /**
     * Default constructor for injection.
     *
     * @param authorizationClient atz client
     * @param splatAwareAuthConfig config
     */
    @Inject
    public SplatAwareBackendServiceAuthContextRequestFilter(jakarta.inject.Provider<AuthorizationClient> authorizationClient,
                                                            jakarta.inject.Provider<SplatAwareAuthConfig> splatAwareAuthConfig) {
        super(authorizationClient.get(),
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
