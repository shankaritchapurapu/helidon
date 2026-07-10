/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.pic.identity.authentication.error.AuthServerUnavailableException;
import com.oracle.pic.identity.authorization.permissions.Permission;
import com.oracle.pic.identity.authorization.sdk.AssociationAuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AssociationAuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

class AuthContextRequestFilterFactoryTest extends BaseAuthenticationClientTest {

    @Test
    void createsSplatAwareFilter() {
        Config config = Config.just(ConfigSources.create(Map.of("oci.identity.splat-aware.region", "us-ashburn-1")));

        AuthContextRequestFilterFactory factory = new AuthContextRequestFilterFactory(config);
        AuthContextRequestFilter filter = factory.create(null, Optional.of(new FakeAuthorizationClient()));

        assertThat(filter, instanceOf(ProvenanceAwareSplatAuthContextRequestFilter.class));
    }

    @Test
    void createsSplatAwareFilterWithoutIdentityConfig() {
        AuthContextRequestFilterFactory factory = new AuthContextRequestFilterFactory(Config.empty());
        AuthContextRequestFilter filter = factory.create(null, Optional.of(new FakeAuthorizationClient()));

        assertThat(filter, instanceOf(ProvenanceAwareSplatAuthContextRequestFilter.class));
    }

    private static final class FakeAuthorizationClient implements IAuthorizationClient {
        @Override
        public AuthorizationResponse makeAuthorizationCall(AuthorizationRequest request)
                throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public AuthorizationResponse makeAuthorizationCall(AuthorizationRequest request, String endpoint)
                throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssociationAuthorizationResponse makeAuthorizationCall(AssociationAuthorizationRequest request)
                throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssociationAuthorizationResponse makeAuthorizationCall(AssociationAuthorizationRequest request,
                                                                     String endpoint)
                throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean preAuthorize(AuthorizationRequest request) throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean authorizeAny(AuthorizationRequest request) throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean authorizeAll(AuthorizationRequest request) throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Permission> authorizeSet(AuthorizationRequest request, Set<Permission> permissions)
                throws AuthServerUnavailableException {
            throw new UnsupportedOperationException();
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
            return "service";
        }
    }
}
