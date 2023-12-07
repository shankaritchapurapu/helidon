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

/** Commons OCI Header Names. */
// inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-common/src/main/java/com/oracle/oci/sfw/netty/common/header/OciHeaderNames.java
class OciHeaderNames {
    public static final String X_DATE = "X-Date";
    // TODO: reconcile this with AuthenticationSupportingFilter#TAG_DEFAULT_HEADER
    public static final String X_CONTENT_SHA256 = "X-Content-SHA256";
    public static final String X_CROSS_TENANCY_REQUEST = "x-cross-tenancy-request";
    public static final String IDEMPOTENCY_TOKEN = "opc-idempotency-token";
    public static final String RETRY_TOKEN = "opc-retry-token";
    public static final String OPC_REQUEST_ID = "opc-request-id";
    public static final String CLIENT_INFO = "opc-client-info";
    public static final String NEXT_PAGE = "opc-next-page";
    public static final String PREVIOUS_PAGE = "opc-previous-page";
    public static final String TOTAL_ITEMS = "opc-total-items";

}
