/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.util.Map;
import java.util.Set;

import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.AddConfigBlock;
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
@AddBean(AuthContextFilterTest.TestResource.class)
@AddConfigBlock(type = "yaml", value =
        // language=yaml
        """
        oci.identity:
          filters:
            auth-context:
              paths:
              - path: /cars/*
              - path: /users/*
        """)
class AuthContextFilterTest extends AbstractAuthorizationTest {

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
                .get()) {

            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(String.class), is("USER 5"));
        }
    }

    @Test
    public void testNotAuthorized(WebTarget target) throws Exception {
        try (var res = target.path("/cars/5")
                .request()
                .header("TEST_USER_NAME", "JOE")
                .get()) {

            assertThat(res.getStatus(), is(404));
        }
    }

    @Test
    public void testNotAuthenticated(WebTarget target) throws Exception {
        try (var res = target.path("/cars/5")
                .request()
                .header("TEST_USER_NAME", "DAN")
                .get()) {

            assertThat(res.getStatus(), is(401));
        }
    }

    @Test
    public void testNotProtected(WebTarget target) throws Exception {
        try (var res = target.path("/rockets/5")
                .request()
                .get()) {

            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(String.class), is("ROCKET 5"));
        }
    }

    @Path("/")
    public static class TestResource {

        // Yes we can shim that!
        @javax.ws.rs.core.Context
        private javax.ws.rs.container.ResourceInfo resourceInfo;

        @GET
        @Path("/users/{userId}")
        @AuthorizationPermission("USER_READ")
        public Response getUser(
                @AuthorizationRequestContext AuthorizationRequest authorizationRequest,
                @PathParam("userId") String userId) {
            assertThat(resourceInfo, notNullValue());
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

        @GET
        @Path("/rockets/{rocketId}")
        public Response getRocket(
                @PathParam("rocketId") String rocketId) {
            return Response.ok().entity("ROCKET " + rocketId).build();
        }
    }
}
