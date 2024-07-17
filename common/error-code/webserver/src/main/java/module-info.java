/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

/**
 * Helidon support for Error Codes for Helidon WebServer.
 */
module com.oracle.helidon.oci.errorcode.webserver {
    requires io.helidon.webserver;
    requires com.oracle.helidon.oci.errorcode;

    exports com.oracle.helidon.oci.errorcode.webserver;

    provides io.helidon.webserver.spi.ServerFeatureProvider
            with com.oracle.helidon.oci.errorcode.webserver.ErrorCodeServerFeatureProvider;
}
