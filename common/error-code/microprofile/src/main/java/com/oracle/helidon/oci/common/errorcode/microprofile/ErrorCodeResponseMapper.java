/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode.microprofile;

import io.helidon.http.Status;

import com.oracle.helidon.oci.common.errorcode.ErrorCode;
import com.oracle.helidon.oci.common.errorcode.ErrorDetail;
import com.oracle.helidon.oci.common.errorcode.RenderableException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Maps a Response to a {@link com.oracle.helidon.oci.common.errorcode.RenderableException} for MicroProfile Rest Client.
 */
public class ErrorCodeResponseMapper implements ResponseExceptionMapper<RenderableException> {

    /**
     * Attempt to convert any response with a code >= 400 to a {@link RenderableException}.
     *
     * @param response the response to convert
     * @return the exception
     * @throws ProcessingException   if a problem is encountered reading the entity
     * @throws IllegalStateException if called after the response has been closed
     */
    @Override
    public RenderableException toThrowable(Response response) {
        // we need to buffer the entity for other mappers
        if (!response.bufferEntity()) {
            throw new ProcessingException("Unable to buffer response entity");
        }

        ErrorDetail errorDetail = response.readEntity(ErrorDetail.class);
        Response.StatusType statusInfo = response.getStatusInfo();
        return new RenderableException(ErrorCode.create(Status.create(statusInfo.getStatusCode(),
                                                                      statusInfo.getReasonPhrase()),
                                                        errorDetail),
                                       errorDetail.getMessage(),
                                       errorDetail.getOriginalMessage(),
                                       errorDetail.getOriginalMessageTemplate(),
                                       errorDetail.getMessageArguments());
    }

    /**
     * Run after message codecs but before {@link Priorities#USER}.
     *
     * @return the priority
     */
    @Override
    public int getPriority() {
        return Priorities.ENTITY_CODER + 1;
    }
}
