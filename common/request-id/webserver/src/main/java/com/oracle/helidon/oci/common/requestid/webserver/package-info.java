/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

/**
 * Server filter that computes correct request ID and adds it as a request header,
 * stores it in context (if enabled) as {@link com.oracle.helidon.oci.common.requestid.OciRequestId},
 * and registers it in {@link io.helidon.logging.common.HelidonMdc} under
 * {@link com.oracle.helidon.oci.common.requestid.OciRequestId#OCI_REQUEST_ID}.
 */
package com.oracle.helidon.oci.common.requestid.webserver;
