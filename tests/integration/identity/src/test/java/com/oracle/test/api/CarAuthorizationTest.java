/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.test.api;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.AddConfig;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import io.helidon.microprofile.testing.mocking.MockBean;

import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.AuthenticatorError;
import com.oracle.pic.identity.authentication.ClaimType;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authentication.PrincipalImpl;
import com.oracle.pic.identity.authentication.PrincipalSerializer;
import com.oracle.pic.identity.authentication.PrincipalSerializerFactory;
import com.oracle.pic.identity.authentication.PrincipalType;
import com.oracle.pic.identity.authentication.SecurityContext;
import com.oracle.pic.identity.authentication.error.AuthServerUnavailableException;
import com.oracle.pic.identity.authorization.permissions.Permission;
import com.oracle.pic.identity.authorization.sdk.AuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.test.model.Car;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.weld.proxy.WeldClientProxy;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@HelidonTest
@AddBean(CarResource.class)
@AddConfig(key = "oci.identity.filters.auth-context.paths.path.0", value = "/v1/cars")
class CarAuthorizationTest {

    @MockBean(answer = Answers.RETURNS_MOCKS)
    protected AuthenticatorClient authenticatorClient;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    protected AuthorizationClient authorizationClient;

    private Map<String, Set<String>> getUsers() {
        return Map.of(
                "BOB", Set.of("CAR_WRITE", "CAR_READ"),
                "JOE", Set.of("CAR_WRITE"),
                "SAM", Set.of("CAR_READ")
        );
    }

    @Test
    @Order(1)
    public void testNotAuthorizedWrite(WebTarget target) throws Exception {
        try (var res = target.path("/v1/cars")
                .request()
                .header("TEST_USER_NAME", "SAM")
                .put(Entity.json(Car.builder().id(8).build()))) {

            assertThat(res.getStatus(), is(404));
            var errorDetail = res.readEntity(com.oracle.helidon.oci.common.errorcode.ErrorDetail.class);
            assertThat(errorDetail.getErrorCode(), is("NotAuthorizedOrNotFound"));
            assertThat(errorDetail.getMessage(), is("Authorization failed or requested resource not found"));
        }
    }

    @Test
    @Order(2)
    public void testAuthorizedWrite(WebTarget target) throws Exception {
        try (var res = target.path("/v1/cars")
                .request()
                .header("TEST_USER_NAME", "BOB")
                .put(Entity.json(Car.builder().id(8).build()))) {

            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(Car.class).getId(), is(8));
        }
    }

    @Test
    @Order(3)
    public void testAuthorizedRead(WebTarget target) throws Exception {
        try (var res = target.path("/v1/cars")
                .request()
                .header("TEST_USER_NAME", "BOB")
                .header("carId", "8")
                .get()) {

            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(Car.class).getId(), is(8));
        }
    }

    @PostConstruct
    protected void mockIt() throws AuthServerUnavailableException {
        when(unwrap(authorizationClient).preAuthorize(any(AuthorizationRequest.class)))
                .thenAnswer(i -> {
                    AuthorizationRequest request = i.getArgument(0);
                    return request.getUserPrincipal()
                            .stream()
                            .map(Principal::getSubjectId)
                            .flatMap(s -> Optional.ofNullable(getUsers().get(s)).stream())
                            .flatMap(Collection::stream)
                            .map(Permission::get)
                            .anyMatch(p -> request.getPermissions().contains(p));
                });
        when(unwrap(authorizationClient).getServiceName()).thenReturn("MOCK_TEST_SERVICE");
        when(unwrap(authenticatorClient).authenticate(anyString(), any(URI.class), anyMap(), any(Optional.class), anyBoolean()))
                .then(m -> userSecurityContext(m.getArgument(2)));
    }

    @SuppressWarnings("unchecked")
    protected static <T> T unwrap(T obj) {
        if (obj instanceof WeldClientProxy proxy) {
            return (T) proxy.getMetadata().getContextualInstance();
        }
        return obj;
    }

    private SecurityContext userSecurityContext(Map<String, List<String>> headers) {
        Optional<String> name = Optional.ofNullable(headers.get("TEST_USER_NAME"))
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .filter(s -> getUsers().containsKey(s));

        Principal principal = mock(Principal.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        if (name.isPresent()) {
            when(principal.getTenantId()).thenReturn(name.get() + "s_TENANT");
            when(principal.getSubjectId()).thenReturn(name.get());
            when(principal.getType()).thenReturn(PrincipalType.USER);
            when(securityContext.principal()).thenReturn(Optional.of(principal));
        } else {
            when(securityContext.error()).then(m -> Optional.of(AuthenticatorError.MISSING_AUTHENTICATION_INFO));
        }

        when(securityContext.isSuccess()).then(m -> name.isPresent());
        return securityContext;
    }
}
