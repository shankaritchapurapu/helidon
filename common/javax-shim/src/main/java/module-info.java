/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxClientBuilder;
import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxRuntimeDelegate;

module helidon.oci.common.javax.shim {
    requires java.ws.rs;
    requires jakarta.ws.rs;
    requires oci.java.sdk.common.httpclient;

    exports com.oracle.helidon.oci.common.javax.jaxrs.shim;

    provides javax.ws.rs.client.ClientBuilder with JavaxClientBuilder;
    provides javax.ws.rs.ext.RuntimeDelegate with JavaxRuntimeDelegate;
}