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
 * Helidon support for Error Codes for MicroProfile applications.
 * <p>
 * Exception mapper for JAX-RS is registered automatically with Jersey.
 *
 * @see com.oracle.helidon.oci.errorcode.microprofile.ErrorCodeResponseMapper
 */
module com.oracle.helidon.oci.errorcode.microprofile {
    requires jakarta.ws.rs;

    requires microprofile.rest.client.api;
    requires io.helidon.microprofile.server;
    requires com.oracle.helidon.oci.errorcode;

    exports com.oracle.helidon.oci.errorcode.microprofile;

    provides org.glassfish.jersey.internal.spi.AutoDiscoverable
            with com.oracle.helidon.oci.errorcode.microprofile.ErrorCodeAutoDiscoverable;

    opens com.oracle.helidon.oci.errorcode.microprofile to weld.core.impl, io.helidon.microprofile.cdi;
}
