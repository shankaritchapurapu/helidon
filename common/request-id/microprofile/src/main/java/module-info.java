/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

/**
 * Helidon support for opc-request-id.
 */
module com.oracle.helidon.oci.common.requestid.microprofile {
    requires transitive com.oracle.helidon.oci.common.requestid;
    requires com.oracle.helidon.oci.common.requestid.webserver;

    // jersey client filter
    requires jakarta.ws.rs;
    requires jersey.common;
    requires jakarta.annotation;
    requires jakarta.cdi;
    requires io.helidon.microprofile.server;

    exports com.oracle.helidon.oci.common.requestid.microprofile;

    provides org.glassfish.jersey.internal.spi.AutoDiscoverable
            with com.oracle.helidon.oci.common.requestid.microprofile.RequestIdAutoDiscoverable;
}
