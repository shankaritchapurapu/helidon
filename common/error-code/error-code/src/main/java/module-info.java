/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

/**
 * Helidon support for Error Codes.
 *
 * @see com.oracle.helidon.oci.common.errorcode.RenderableException
 */
module com.oracle.helidon.oci.common.errorcode {
    requires io.helidon.http;

    // only annotations are used from these packages
    requires static com.fasterxml.jackson.annotation;
    requires static jakarta.json.bind;

    exports com.oracle.helidon.oci.common.errorcode;
}
