/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.tagging;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.service.registry.Service;

import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.requests.CreateTagRequest;
import com.oracle.bmc.identity.responses.CreateTagResponse;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.SecurityContext;
import com.oracle.pic.identity.authentication.metrics.AuthMetrics;
import com.oracle.pic.identity.authorization.permissions.Permission;
import com.oracle.pic.identity.authorization.sdk.AssociationAuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AssociationAuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;

final class TaggingEndpointTestServices {
    static final TestAuthenticatorClient AUTHENTICATOR = new TestAuthenticatorClient();
    static final TestAuthorizationClient AUTHORIZATION = new TestAuthorizationClient();
    static final TestIdentity IDENTITY = new TestIdentity();

    private TaggingEndpointTestServices() {
    }

    static void reset() {
        AUTHENTICATOR.reset();
        AUTHORIZATION.reset();
        IDENTITY.reset();
    }
}

final class TestAuthenticatorClient extends AuthenticatorClient {
    private SecurityContext securityContext;

    void reset() {
        securityContext = null;
    }

    void securityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public SecurityContext authenticate(String requestTarget,
                                        URI uri,
                                        Map<String, List<String>> headers,
                                        Optional<String> body) {
        return securityContext;
    }

    @Override
    public SecurityContext authenticate(String requestTarget,
                                        URI uri,
                                        Map<String, List<String>> headers,
                                        Optional<String> body,
                                        boolean allowServicePrincipal) {
        return securityContext;
    }

    @Override
    public boolean useUnencodedQueryParameterForSignatureVerification() {
        return false;
    }

    @Override
    public AuthMetrics getAuthMetrics() {
        return null;
    }

    @Override
    public SecurityContext authenticateCasper(String requestTarget,
                                              URI uri,
                                              Map<String, List<String>> headers,
                                              Optional<String> body) {
        return securityContext;
    }

    @Override
    public SecurityContext authenticateOboToken(String oboToken) {
        return securityContext;
    }

    @Override
    public void close() {
    }
}

final class TestAuthorizationClient implements IAuthorizationClient {
    private final List<AuthorizationRequest> authorizationRequests = new ArrayList<>();
    private final List<AuthorizationRequest> preAuthorizeRequests = new ArrayList<>();
    private AuthorizationResponse authorizationResponse;

    void reset() {
        authorizationRequests.clear();
        preAuthorizeRequests.clear();
        authorizationResponse = null;
    }

    void authorizationResponse(AuthorizationResponse authorizationResponse) {
        this.authorizationResponse = authorizationResponse;
    }

    List<AuthorizationRequest> authorizationRequests() {
        return List.copyOf(authorizationRequests);
    }

    List<AuthorizationRequest> preAuthorizeRequests() {
        return List.copyOf(preAuthorizeRequests);
    }

    @Override
    public AuthorizationResponse makeAuthorizationCall(AuthorizationRequest authorizationRequest) {
        authorizationRequests.add(authorizationRequest);
        return authorizationResponse;
    }

    @Override
    public AuthorizationResponse makeAuthorizationCall(AuthorizationRequest authorizationRequest, String oboToken) {
        return makeAuthorizationCall(authorizationRequest);
    }

    @Override
    public AssociationAuthorizationResponse makeAuthorizationCall(
            AssociationAuthorizationRequest authorizationRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssociationAuthorizationResponse makeAuthorizationCall(AssociationAuthorizationRequest authorizationRequest,
                                                                 String oboToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean preAuthorize(AuthorizationRequest authorizationRequest) {
        preAuthorizeRequests.add(authorizationRequest);
        return true;
    }

    @Override
    public boolean authorizeAny(AuthorizationRequest authorizationRequest) {
        return true;
    }

    @Override
    public boolean authorizeAll(AuthorizationRequest authorizationRequest) {
        return true;
    }

    @Override
    public Set<Permission> authorizeSet(
            AuthorizationRequest authorizationRequest,
            Set<Permission> permissions) {
        return permissions;
    }

    @Override
    public void close() {
    }

    @Override
    public String getRegion() {
        return "us-ashburn-1";
    }

    @Override
    public String getPhysicalAD() {
        return "AD-1";
    }

    @Override
    public String getServiceName() {
        return "helidon-tagging-example";
    }
}

final class TestIdentity {
    private final List<CreateTagRequest> createTagRequests = new ArrayList<>();
    private final Identity client = (Identity) Proxy.newProxyInstance(Identity.class.getClassLoader(),
                                                                     new Class<?>[] {Identity.class},
                                                                     this::invoke);
    private CreateTagResponse createTagResponse;

    void reset() {
        createTagRequests.clear();
        createTagResponse = null;
    }

    Identity client() {
        return client;
    }

    void createTagResponse(CreateTagResponse createTagResponse) {
        this.createTagResponse = createTagResponse;
    }

    List<CreateTagRequest> createTagRequests() {
        return List.copyOf(createTagRequests);
    }

    private Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        if ("createTag".equals(methodName)) {
            createTagRequests.add((CreateTagRequest) args[0]);
            return createTagResponse;
        }
        return defaultValue(proxy, method, args);
    }

    private static Object defaultValue(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
        case "close", "refreshClient", "setEndpoint", "setRegion", "useRealmSpecificEndpointTemplate" -> null;
        case "getEndpoint" -> "http://localhost";
        case "toString" -> "TestIdentity";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == args[0];
        default -> primitiveDefault(method.getReturnType());
        };
    }

    private static Object primitiveDefault(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}

@Weight(1000.0)
@Service.Singleton
class TestAuthenticatorClientSupplier implements Supplier<AuthenticatorClient> {
    @Override
    public AuthenticatorClient get() {
        return TaggingEndpointTestServices.AUTHENTICATOR;
    }
}

@Weight(1000.0)
@Service.Singleton
class TestAuthorizationClientSupplier implements Supplier<IAuthorizationClient> {
    @Override
    public IAuthorizationClient get() {
        return TaggingEndpointTestServices.AUTHORIZATION;
    }
}

@Weight(1000.0)
@Service.Singleton
class TestIdentitySupplier implements Supplier<Identity> {
    @Override
    public Identity get() {
        return TaggingEndpointTestServices.IDENTITY.client();
    }
}
