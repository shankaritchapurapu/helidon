/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

/**
 * This class encapsulates the names of headers commonly used in Identity.
 **/
// inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-identity/src/main/java/com/oracle/oci/sfw/netty/identity/headers/IdentityHeaders.java
class IdentityHeaders {
    //    public static final String AUTHORIZATION = "authorization";
    //    public static final String DATE = "date";
    static final String CONTENT_TYPE = "content-type";
    //    public static final String CONTENT_LENGTH = "content-length";
    static final String HOST = "host";
    private IdentityHeaders() {
    }
    //    public static final String X_CONTENT_SHA256 = OciHeaderNames.X_CONTENT_SHA256;
}
