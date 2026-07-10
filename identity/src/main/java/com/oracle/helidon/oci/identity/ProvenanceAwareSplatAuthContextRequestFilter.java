/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import javax.ws.rs.container.ContainerRequestContext;

import com.oracle.helidon.oci.jaxrs.HelidonContainerRequestContext;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.SplatAwareAuthContextRequestFilter;
import com.oracle.pic.identity.authorization.sdk.config.SplatAwareAuthConfig;

/**
 * Records trusted SPLAT provenance when the Auth SDK identifies a request using its port and certificate checks.
 */
final class ProvenanceAwareSplatAuthContextRequestFilter extends SplatAwareAuthContextRequestFilter {
    private static final String SPLAT_REQUEST_VALIDATED_CONTEXT_KEY =
            "com.oracle.helidon.oci.splat.requestValidated";

    ProvenanceAwareSplatAuthContextRequestFilter(AuthenticatorClient authenticatorClient,
                                                 IAuthorizationClient authorizationClient,
                                                 SplatAwareAuthConfig splatAwareAuthConfig,
                                                 Region region) {
        super(authenticatorClient, authorizationClient, splatAwareAuthConfig, region);
    }

    @Override
    protected boolean isSplatRequest(ContainerRequestContext requestContext) {
        boolean splatRequest = super.isSplatRequest(requestContext);
        if (splatRequest
                && splatAwareAuthConfig.isValidateSplatCert()
                && requestContext instanceof HelidonContainerRequestContext helidonContext) {
            helidonContext.getServerRequest().context().register(SPLAT_REQUEST_VALIDATED_CONTEXT_KEY, Boolean.TRUE);
        }
        return splatRequest;
    }
}
