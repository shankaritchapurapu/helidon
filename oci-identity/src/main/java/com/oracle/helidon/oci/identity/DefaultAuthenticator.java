/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;

import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.SecurityContext;

@SuppressWarnings("unused")
class DefaultAuthenticator implements Authenticator {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationSupportingFilter.class.getName());

    private final ContainerRequestContextSupplier containerRequestContextSupplier;
    private final AuthenticatorClient authenticatorClient;

    @Inject
    @SuppressWarnings("unused")
    DefaultAuthenticator(AuthenticatorClient authenticatorClient,
                         ContainerRequestContextSupplier containerRequestContextSupplier) {
        this.authenticatorClient = Objects.requireNonNull(authenticatorClient);
        this.containerRequestContextSupplier = Objects.requireNonNull(containerRequestContextSupplier);
    }

    @Override
    // inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-identity/src/main/java/com/oracle/oci/sfw/netty/identity/authentication/Authenticator.java
    public SecurityContext authenticateRequest() {
        ContainerRequestContext rc = Objects.requireNonNull(containerRequestContextSupplier.get(),
        "Expected containerRequestContext to be available but was not.");

        // Step 2: Extract signed headers from client request
        Map<String, List<String>> headers = rc.getHeaders();

        // Step 3: Determine if we need to include the body in the case of a GET
        URI methodUri = rc.getUriInfo().getRequestUri();
        String opcRequestId = rc.getHeaderString(OciHeaderNames.OPC_REQUEST_ID);
        String shaDigest = rc.getHeaderString(AuthenticationSupportingFilter.TAG_DEFAULT_HEADER);
        // TODO: https://jira.oci.oraclecorp.com/browse/WLMS-852
        boolean allowBodyForGet = rc.getMethod().equals(HttpMethod.GET) && (shaDigest != null);

        // Step 4: Perform AuthN using client's Signed Headers and compare body with header's
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE,
                       "Proceeding to authenticate request {0} with signed headers and sha digest {1}",
                       new Object[] {opcRequestId, shaDigest});
        }
        SecurityContext authenticationResponse =
                authenticatorClient.authenticate(
                        rc.getMethod(),
                        methodUri,
                        headers,
                        Optional.ofNullable(shaDigest),
                        allowBodyForGet);
        LOGGER.log(Level.FINE, "AuthN call performed successfully; success={0}", authenticationResponse.isSuccess());
        return authenticationResponse;
    }

}
