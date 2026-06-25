/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.service.registry.Service;

import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.AuthenticatorError;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authentication.PrincipalImpl;
import com.oracle.pic.identity.authentication.SecurityContext;
import com.oracle.pic.identity.authentication.metrics.AuthMetrics;
import com.oracle.pic.identity.authorization.permissions.Permission;
import com.oracle.pic.identity.authorization.sdk.AssociationAuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AssociationAuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;

final class DataPlaneEndpointTestServices {
    static final Principal PRINCIPAL = new PrincipalImpl(
            "ocid1.tenancy.oc1..aaaaaaaadataplanetenant",
            "ocid1.user.oc1..aaaaaaaadataplaneuser");

    private DataPlaneEndpointTestServices() {
    }
}

final class TestAuthenticatorClient extends AuthenticatorClient {
    private static final SecurityContext SECURITY_CONTEXT = new SecurityContext() {
        @Override
        public Optional<Principal> principal() {
            return Optional.of(DataPlaneEndpointTestServices.PRINCIPAL);
        }

        @Override
        public Optional<AuthenticatorError> error() {
            return Optional.empty();
        }

        @Override
        public String errorMessage() {
            return null;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    };

    @Override
    public SecurityContext authenticate(String requestTarget,
                                        URI uri,
                                        Map<String, List<String>> headers,
                                        Optional<String> body) {
        return SECURITY_CONTEXT;
    }

    @Override
    public SecurityContext authenticate(String requestTarget,
                                        URI uri,
                                        Map<String, List<String>> headers,
                                        Optional<String> body,
                                        boolean allowServicePrincipal) {
        return SECURITY_CONTEXT;
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
        return SECURITY_CONTEXT;
    }

    @Override
    public SecurityContext authenticateOboToken(String oboToken) {
        return SECURITY_CONTEXT;
    }

    @Override
    public void close() {
    }
}

final class TestAuthorizationClient implements IAuthorizationClient {
    @Override
    public AuthorizationResponse makeAuthorizationCall(AuthorizationRequest authorizationRequest) {
        return null;
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
    public Set<Permission> authorizeSet(AuthorizationRequest authorizationRequest, Set<Permission> permissions) {
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
        return "helidon-data-plane-example";
    }
}

@Weight(1000.0)
@Service.Singleton
class TestAuthenticatorClientSupplier implements Supplier<AuthenticatorClient> {
    @Override
    public AuthenticatorClient get() {
        return new TestAuthenticatorClient();
    }
}

@Weight(1000.0)
@Service.Singleton
class TestAuthorizationClientSupplier implements Supplier<IAuthorizationClient> {
    @Override
    public IAuthorizationClient get() {
        return new TestAuthorizationClient();
    }
}
