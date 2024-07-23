/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode;

import io.helidon.http.Status;

record ErrorCodeRecord(String errorCode, String errorMessage, Status status) implements ErrorCode {
}
