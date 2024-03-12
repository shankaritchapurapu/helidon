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
package com.oracle.helidon.oci.requestid;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.oracle.pic.commons.rid.RequestIdUtils;

import io.helidon.common.context.Contexts;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;

/**
 * Forwards the opc-request-id header value on every REST client request. Only
 * 2 of the 3 parts are forwarded: the so-called downstream request ID.
 */
@Priority(Priorities.AUTHENTICATION - 100)
class RequestIdClientFilter implements ClientRequestFilter {
    private static final Logger LOGGER = Logger.getLogger(ClientRequestFilter.class.getName());

    @Override
    public void filter(ClientRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(OciHeaderNames.OPC_REQUEST_ID);

        // if empty or more than 2-tuple, fix it
        if (requestId == null || requestId.isEmpty() || requestId.split("/").length > 2) {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            if (requestId != null) {
                headers.remove(OciHeaderNames.OPC_REQUEST_ID);
            }

            // find request id in our context and use it
            Contexts.context().flatMap(c -> c.get(Request.class, String.class))
                    .ifPresent(id -> {
                        String clientRequestId = RequestIdUtils.getDownStreamRequestId(id);
                        if (clientRequestId != null) {
                            headers.add(OciHeaderNames.OPC_REQUEST_ID, clientRequestId);
                        } else {
                            LOGGER.log(Level.WARNING, "Cannot forward opc-request-id header");
                        }
                    });
        }
    }
}
