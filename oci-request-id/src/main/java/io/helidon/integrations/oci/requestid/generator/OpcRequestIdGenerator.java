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

package io.helidon.integrations.oci.requestid.generator;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestFilter;

import com.oracle.pic.commons.rid.RequestIdUtils;
//import io.netty.handler.codec.http.FullHttpRequest;
//import lombok.extern.slf4j.Slf4j;

/**
 * Note: this code was forked from com.oracle.oci.sfw.netty.request.id.OpcRequestIdGenerator.
 *
 * Generates a properly formatted opcRequestId. For details see -
 * https://confluence.oci.oraclecorp.com/x/ngFY
 */
//@Slf4j
public class OpcRequestIdGenerator {
    private static final Logger LOGGER = Logger.getLogger(OpcRequestIdGenerator.class.getName());

    private static final String REQUEST_ID_FORMAT = "%s/%s/%s";
    public static final String OPC_REQUEST_ID_HEADER = RequestIdUtils.OPC_REQUEST_ID_HEADER;
    public static final String DELIMITER = RequestIdUtils.DELIMITER;

    /** Maximum length of the client-provided id (CID). Ids longer than this will be truncated. */
    public static final int MAX_CID_LENGTH = RequestIdUtils.MAX_CID_LENGTH;

//    /**
//     * Returns a properly formatted opcRequestId.
//     *
//     * @param request FullHttpRequest object.
//     * @return a properly formatted and stripped request id.
//     */
//    public static String getOpcRequestId(FullHttpRequest request) {
//        String requestId = request.headers().get(OPC_REQUEST_ID_HEADER);
//        return getOpcRequestId(requestId);
//    }

    /**
     * Returns a properly formatted opcRequestId.
     *
     * @param existingId The existing id
     * @return a properly formatted and stripped request id.
     */
    public static String getOpcRequestId(String existingId) {
        if (existingId == null || existingId.isEmpty()) {
            // Client did not provide id: <empty>/call_stack_id/individual_request_id
            return String.format(REQUEST_ID_FORMAT, "", generateUniqueId(), generateUniqueId());
        } else {
            String[] parts = existingId.split(DELIMITER);
            switch (parts.length) {
            case 0:
                // id which results in parts being empty but passes initial check, such as "////////////"
                return String.format(REQUEST_ID_FORMAT, "", generateUniqueId(), generateUniqueId());
            case 1:
                // Since this is a non-empty string, it will have at least one part
                // client_id/new_call_stack_id/new_individual_request_id
                return String.format(
                        REQUEST_ID_FORMAT, truncate(parts[0]), generateUniqueId(), generateUniqueId());
            case 2:
                // One service making another service call
                // client_id/existing_call_stack_id/new_individual_request_id
                return String.format(
                        REQUEST_ID_FORMAT, truncate(parts[0]), truncate(parts[1]), generateUniqueId());
            default:
                // There are 3 or more parts. Either client provided an id with '/' in it
                // or a service is not doing the right thing and adding more parts than it is supposed to.
                LOGGER.log(Level.WARNING, "Invalid request id '{0}'", existingId);
                return String.format(
                        REQUEST_ID_FORMAT, truncate(parts[0]), generateUniqueId(), generateUniqueId());
            }
        }
    }

    /**
     * Returns a String truncated to the first com.oracle.pic.commons.rid.RequestIdUtils.MAX_CID_LENGTH
     * characters of the specified String.
     */
    private static String truncate(String part) {
        if (part.length() > MAX_CID_LENGTH) {
            String truncated = part.substring(0, MAX_CID_LENGTH);
            LOGGER.log(Level.WARNING, "Truncated client request id from {0} to {1}", new String[] {part, truncated});
            return truncated;
        }
        return part;
    }

    private static String generateUniqueId() {
        return RequestIdUtils.generateUniqueId();
    }
}
