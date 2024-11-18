/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.util.Map;
import java.util.Objects;

import io.helidon.config.Config;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;

/**
 * Responsible for installing configured identity filters.
 */
@ConstrainedTo(RuntimeType.SERVER)
public class AuthenticationSupportAutoDiscoverable implements AutoDiscoverable {

    private static final System.Logger LOGGER = System.getLogger(AuthenticationSupportAutoDiscoverable.class.getName());

    private static final Map<String, Class<?>> FILTERS = Map.of(
            AuthContextRequestFilter.NAME, AuthContextRequestFilter.class,
            BackendServiceAuthContextRequestFilter.NAME, BackendServiceAuthContextRequestFilter.class,
            OauthContextRequestFilter.NAME, OauthContextRequestFilter.class,
            SplatAuthorizationVerificationFilter.NAME, SplatAuthorizationVerificationFilter.class,
            SplatAwareAuthContextRequestFilter.NAME, SplatAwareAuthContextRequestFilter.class,
            SplatAwareBackendServiceAuthContextRequestFilter.NAME, SplatAwareBackendServiceAuthContextRequestFilter.class
    );

    @Override
    public void configure(FeatureContext context) {
        RuntimeType rt = context.getConfiguration().getRuntimeType();
        if (RuntimeType.SERVER == rt) {
            context.register(IdentityBinder.class);

            Config config = CDI.current().select(Config.class).get();
            Config filters = config.get("oci.identity.filters");
            filters.traverse()
                    .filter(config1 -> !config1.isLeaf())
                    .map(fc -> {
                        Class<?> filter = FILTERS.get(fc.key().name());
                        if (filter == null) {
                            LOGGER.log(System.Logger.Level.WARNING,
                                       () -> "Filter "
                                               + fc.key().name()
                                               + " not found, known identity filters are "
                                               + FILTERS.keySet());
                        }
                        return filter;
                    })
                    .filter(Objects::nonNull)
                    .forEach(c -> context.register(c, Priorities.AUTHENTICATION));
        }
    }
}

