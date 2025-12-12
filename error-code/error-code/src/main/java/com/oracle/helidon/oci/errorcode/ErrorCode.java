/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.errorcode;

import java.util.Objects;

import io.helidon.http.Status;

/**
 * Each error code needs to have an {@link #errorCode()}, and a {@link #errorMessage()}, and it is associated
 * with a specific {@link #status()}.
 * <p>
 * Most commonly used error codes can be found in {@link ErrorCodes}
 */
public interface ErrorCode {
    /**
     * Create error info from a response from a remote server.
     *
     * @param status  HTTP status received
     * @param details ErrorDetails read from the entity
     * @return error information
     */
    static ErrorCode create(Status status, ErrorDetail details) {
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(details, "Details cannot be null");

        try {
            return ErrorCodes.valueOf(details.getErrorCode());
        } catch (IllegalArgumentException e) {
            // custom deserialized error infos will be 500 errors, so we can construct them
            return new ErrorCodeRecord(details.getErrorCode(),
                                       details.getMessage(),
                                       status);
        }
    }

    /**
     * HTTP status (including reason phrase) to use to create a response.
     *
     * @return status to send back to caller
     */
    Status status();

    /**
     * Error code string.
     * It is expected to be one of the enum names from {@link ErrorCodes}.
     * The enum implements this interface.
     *
     * @return error code name
     */
    String errorCode();

    /**
     * Associated (detailed) error message to be sent in the response entity.
     * This may differ from reason phrase of the status.
     *
     * @return error message
     */
    String errorMessage();
}
