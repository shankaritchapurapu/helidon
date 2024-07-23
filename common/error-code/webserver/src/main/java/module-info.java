/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

import com.oracle.helidon.oci.common.errorcode.webserver.ErrorCodeServerFeatureProvider;

/**
 * Helidon support for Error Codes for Helidon WebServer.
 */
module com.oracle.helidon.oci.common.errorcode.webserver {
    requires io.helidon.webserver;
    requires com.oracle.helidon.oci.common.errorcode;

    exports com.oracle.helidon.oci.common.errorcode.webserver;

    provides io.helidon.webserver.spi.ServerFeatureProvider
            with ErrorCodeServerFeatureProvider;
}
