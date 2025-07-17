/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode.microprofile;

import com.oracle.helidon.oci.common.errorcode.ErrorCode;
import com.oracle.helidon.oci.common.errorcode.RenderableException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Maps a {@link com.oracle.helidon.oci.common.errorcode.RenderableException} to a response with JSON payload as described in
 * <a href="https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Error+Codes">
 * Error Codes</a>.
 */
public class ErrorCodeExceptionMapper implements ExceptionMapper<RenderableException> {

    @Override
    public Response toResponse(RenderableException exception) {
        ErrorCode errorCode = exception.errorCode();
        return Response.status(errorCode.status().code(), errorCode.status().reasonPhrase())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(exception.errorDetail())
                .build();
    }
}
