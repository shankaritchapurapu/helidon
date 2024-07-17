/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
