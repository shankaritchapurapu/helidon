/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
