/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

/**
 * Helidon support for opc-request-id.
 *
 * @see com.oracle.helidon.oci.common.requestid.OciRequestId
 */
module com.oracle.helidon.oci.common.requestid {
    // OCI request id support
    requires request.id;

    exports com.oracle.helidon.oci.common.requestid;
}
