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
import com.oracle.pic.identity.authorization.sdk.filter.AuthAnnotationClientFactory;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;

import static com.oracle.helidon.oci.identity.IdentityPathMatchingRequestFilter.loadPathMatchers;

/**
 * A jersey container filters for authentication and authorization.
 * Authentication is mandatory, authorization is not. So you must specify an instance of AuthenticatorClient
 */
@ConstrainedTo(RuntimeType.SERVER)
public class AuthContextRequestFilter extends com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter
        implements IdentityPathMatchingRequestFilter {
    /**
     * Configuration name of this filter.
     */
    public static final String NAME = "auth-context";
    private static final LazyValue<List<PathMatcher>> MATCHERS = LazyValue.create(() -> loadPathMatchers(NAME));
    @Inject
    private jakarta.inject.Provider<AuthorizationClient> authorizationClientProvider;
    @Inject
    private jakarta.inject.Provider<AuthenticatorClient> authenticatorClientProvider;

    /**
     * Default constructor to make sub-classes happy.
     */
    protected AuthContextRequestFilter() {
        super();
    }

    /**
     * Set up clients post construct.
     */
    @PostConstruct
    public void postConstruct() {
        if (this.authenticatorClientProvider.get() == null || this.authorizationClientProvider.get() == null) {
            throw new NullPointerException("At least AuthN or AuthZ must be defined");
        }
        super.authorizationClient = this.authorizationClientProvider.get();
        super.authenticatorClient = this.authenticatorClientProvider.get();
        super.authAnnotationClient = AuthAnnotationClientFactory.getClient();

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
