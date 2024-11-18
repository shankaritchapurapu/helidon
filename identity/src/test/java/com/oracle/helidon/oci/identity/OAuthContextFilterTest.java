/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.util.Map;
import java.util.Set;

import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.AddConfig;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.context.AuthorizationRequestContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@HelidonTest
@AddBean(OAuthContextFilterTest.TestResource.class)
@AddConfig(key = "oci.identity.filters.oauth-context.paths.path.0", value = "/cars/*")
@AddConfig(key = "oci.identity.filters.oauth-context.paths.path.1", value = "/users/*")
public class OAuthContextFilterTest extends AbstractAuthorizationTest {

    @Override
    protected Map<String, Set<String>> getUsers() {
        return Map.of(
                "BOB", Set.of("USER_READ", "CAR_READ"),
                "JOE", Set.of("USER_READ"),
                "SAM", Set.of("CAR_READ")
        );
    }

    @Test
    public void testAuthorized(WebTarget target) throws Exception {
        try (var res = target.path("/users/5")
                .request()
                .header("TEST_USER_NAME", "BOB")
                .header("TEST_PRINCIPAL_TYPE", "NO-oauth")
                .get()) {

            // No oauth and authenticated is passed to AuthContextRequestFilter
            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(String.class), is("USER 5"));
        }
    }

    @Test
    public void testNotAuthorized(WebTarget target) throws Exception {
        try (var res = target.path("/cars/5")
                .request()
                .header("TEST_USER_NAME", "JOE")
                .header("TEST_PRINCIPAL_TYPE", "oauth")
                .get()) {

            // OauthContextRequestFilter bypasses authorization when PRINCIPAL_TYPE = oauth
            assertThat(res.getStatus(), is(200));
        }
    }

    @Test
    public void testNotAuthenticated(WebTarget target) throws Exception {
        try (var res = target.path("/cars/5")
                .request()
                .header("TEST_USER_NAME", "DAN")
                .get()) {

            // OauthContextRequestFilter doesn't throw anything when authentication fails
            assertThat(res.getStatus(), is(200));
        }
    }

    @Path("/")
    public static class TestResource {

        @GET
        @Path("/users/{userId}")
        @AuthorizationPermission("USER_READ")
        public Response getUser(
                @AuthorizationRequestContext AuthorizationRequest authorizationRequest,
                @PathParam("userId") String userId) {
            assertThat(authorizationRequest, notNullValue());
            assertThat(authorizationRequest.getUserPrincipal().get().getSubjectId(), is("BOB"));
            return Response.ok().entity("USER " + userId).build();
        }

        @GET
        @Path("/cars/{userId}")
        @AuthorizationPermission("CAR_READ")
        public Response getCar(
                @AuthorizationRequestContext AuthorizationRequest authorizationRequest,
                @PathParam("userId") String userId) {
            return Response.ok().entity("CAR " + userId).build();
        }
    }
}
