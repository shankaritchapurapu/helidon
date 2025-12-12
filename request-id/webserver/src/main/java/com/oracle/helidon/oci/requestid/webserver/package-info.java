/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

/**
 * Server filter that computes correct request ID and adds it as a request header,
 * stores it in context (if enabled) as {@link com.oracle.helidon.oci.requestid.OciRequestId},
 * and registers it in {@link io.helidon.logging.common.HelidonMdc} under
 * {@link com.oracle.helidon.oci.requestid.OciRequestId#OCI_REQUEST_ID}.
 */
package com.oracle.helidon.oci.requestid.webserver;
