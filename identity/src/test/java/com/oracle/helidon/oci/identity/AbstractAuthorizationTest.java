/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import jakarta.annotation.PostConstruct;
import org.jboss.weld.proxy.WeldClientProxy;
import org.mockito.Answers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class AbstractAuthorizationTest {

    protected PrincipalSerializer serializer = PrincipalSerializerFactory.create();

    @MockBean(answer = Answers.RETURNS_MOCKS)
    protected AuthenticatorClient authenticatorClient;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    protected AuthorizationClient authorizationClient;

    protected abstract Map<String, Set<String>> getUsers();

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

    protected String serializedPrincipal(String principalName) {
        PrincipalImpl principal = new PrincipalImpl("BOBs_TENANT", principalName);
        return serializer.serialize(principal).get();
    }

    private SecurityContext userSecurityContext(Map<String, List<String>> headers) {
        Optional<String> name = Optional.ofNullable(headers.get("TEST_USER_NAME"))
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .filter(s -> getUsers().containsKey(s));

        Optional<String> principalType = Optional.ofNullable(headers.get("TEST_PRINCIPAL_TYPE"))
                .stream()
                .flatMap(Collection::stream)
                .findFirst();

        Principal principal = mock(Principal.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(principal.getClaimValue(ClaimType.PRINCIPAL_TYPE))
                .thenReturn(principalType);

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
