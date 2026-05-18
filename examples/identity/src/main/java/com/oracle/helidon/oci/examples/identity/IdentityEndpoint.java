/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.identity;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.helidon.oci.identity.IdentityContext;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;

import static com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter.PIC_PRINCIPAL;
import static java.lang.System.Logger.Level;

@SuppressWarnings("deprecation")
@RestServer.Endpoint
@Http.Path("/identity")
@Service.Singleton
class IdentityEndpoint {
    private static final System.Logger LOGGER = System.getLogger(IdentityEndpoint.class.getName());

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String ping() {
        return "pong";
    }

    @Http.POST
    @Http.Consumes(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Path("once")
    @AuthorizationPermission("IDENTITY_ONCE")
    String once(@Http.Entity String message, IdentityContext identityContext) {
        LOGGER.log(Level.DEBUG, "Resource method 'once' called for " + principal(identityContext).getSubjectId());
        return message;
    }

    @Http.POST
    @Http.Consumes(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Path("twice")
    @AuthorizationPermission("IDENTITY_TWICE")
    String twice(@Http.Entity String message, IdentityContext identityContext) {
        LOGGER.log(Level.DEBUG, "Resource method 'twice' called for " + principal(identityContext).getSubjectId());
        return message + message;
    }

    /**
     * Access information about the principal that was authenticated/authorized
     * to access a resource.
     *
     * @param identityContext the current request identity context
     * @return the principal
     */
    private Principal principal(IdentityContext identityContext) {
        return (Principal) identityContext.get(PIC_PRINCIPAL);
    }
}
