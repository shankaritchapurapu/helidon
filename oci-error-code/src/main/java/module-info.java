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

import com.oracle.helidon.oci.errorcode.ErrorCodeAutoDiscoverable;

/**
 * Helidon support for Error Codes.
 */
module com.oracle.helidon.oci.errorcode {
    requires java.ws.rs;

    requires microprofile.rest.client.api;

    requires io.helidon.microprofile.server;

    requires com.fasterxml.jackson.annotation;

    opens com.oracle.helidon.oci.errorcode to weld.core.impl, io.helidon.microprofile.cdi;

    exports com.oracle.helidon.oci.errorcode;

    provides org.glassfish.jersey.internal.spi.AutoDiscoverable
            with ErrorCodeAutoDiscoverable;
}
