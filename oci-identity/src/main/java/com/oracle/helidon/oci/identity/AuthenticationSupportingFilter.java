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

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

/**
 * Makes the header value representing the SHA hash digest of the request.
 */
// inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-identity/src/main/java/com/oracle/oci/sfw/netty/identity/authentication/Authenticator.java
@PreMatching
@Priority(Priorities.AUTHENTICATION - 50)
public class AuthenticationSupportingFilter implements ContainerRequestFilter {
    static final String TAG_HEADER = "X-HELIDON-DIGEST";


    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        // determine if we need to include the body in the case of a GET
        boolean hasBody = rc.hasEntity();
//        boolean allowBodyForGet = hasBody && (HttpMethod.GET.equals(rc.getMethod()));

        if (hasBody) {
            RepeatableInputStreamer.Stream stream = RepeatableInputStreamer.create(rc.getEntityStream());
            RepeatableInputStreamer.ReplayStream replayStream = stream.replay();

            String digest = DigestStreamer.calculateDigest(stream);
            rc.getHeaders().add(TAG_HEADER, digest);

            rc.setEntityStream(replayStream);
        }
    }

}
