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
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authorization.sdk.AuthorizationClient;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;

import static com.oracle.helidon.oci.identity.IdentityPathMatchingRequestFilter.loadPathMatchers;

/**
 * An extension of the {@linkplain AuthContextRequestFilter} class to Authenticate using Oauth Exchange Tokens
 * that are generated using
 * {@linkplain com.oracle.pic.identity.authentication.OauthExchangeTokenAuthenticationDetailsProvider
 * OauthExchangeTokenAuthenticationDetailsProvider}.
 */
@Priority(Priorities.AUTHENTICATION)
public class OauthContextRequestFilter
        extends com.oracle.pic.identity.authorization.sdk.OauthContextRequestFilter
        implements IdentityPathMatchingRequestFilter {

    /**
     * Configuration name of this filter.
     */
    public static final String NAME = "oauth-context";
    private static final LazyValue<List<PathMatcher>> MATCHERS =
            LazyValue.create(() -> loadPathMatchers(NAME));

    /**
     * Constructor that allows for authentication, authorization or both.
     *
     * @param authenticatorClient used to initialize authentication
     * @param authorizationClient used to initialize authorization
     */
    @Inject
    public OauthContextRequestFilter(Provider<AuthenticatorClient> authenticatorClient,
                                     Provider<AuthorizationClient> authorizationClient) {
        super(authenticatorClient.get(), authorizationClient.get());
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
