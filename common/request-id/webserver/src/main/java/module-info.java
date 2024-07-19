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

/**
 * Helidon webserver support for opc-request-id.
 * <p>
 * This module provides a feature that adds server filter that computes correct request
 * ID and adds it as a request header,
 * stores it in context (if enabled) as {@link com.oracle.helidon.oci.common.requestid.OciRequestId},
 * and registers it in {@link io.helidon.logging.common.HelidonMdc} under
 * {@link com.oracle.helidon.oci.common.requestid.OciRequestId#OCI_REQUEST_ID}.
 */
module com.oracle.helidon.oci.common.requestid.webserver {
    requires transitive com.oracle.helidon.oci.common.requestid;

    requires io.helidon.common.config;
    requires io.helidon.webserver;
    requires io.helidon.http;
    requires io.helidon.logging.common;

    exports com.oracle.helidon.oci.common.requestid.webserver;

    provides io.helidon.webserver.spi.ServerFeatureProvider
            with com.oracle.helidon.oci.common.requestid.webserver.RequestIdServerFeatureProvider;
}
