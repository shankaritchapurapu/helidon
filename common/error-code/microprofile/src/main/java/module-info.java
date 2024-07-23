/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

import com.oracle.helidon.oci.common.errorcode.microprofile.ErrorCodeAutoDiscoverable;

/**
 * Helidon support for Error Codes for MicroProfile applications.
 * <p>
 * Exception mapper for JAX-RS is registered automatically with Jersey.
 *
 * @see com.oracle.helidon.oci.common.errorcode.microprofile.ErrorCodeResponseMapper
 */
module com.oracle.helidon.oci.common.errorcode.microprofile {
    requires jakarta.ws.rs;

    requires microprofile.rest.client.api;
    requires io.helidon.microprofile.server;
    requires com.oracle.helidon.oci.common.errorcode;

    exports com.oracle.helidon.oci.common.errorcode.microprofile;

    provides org.glassfish.jersey.internal.spi.AutoDiscoverable
            with ErrorCodeAutoDiscoverable;

    opens com.oracle.helidon.oci.common.errorcode.microprofile to weld.core.impl, io.helidon.microprofile.cdi;
}
