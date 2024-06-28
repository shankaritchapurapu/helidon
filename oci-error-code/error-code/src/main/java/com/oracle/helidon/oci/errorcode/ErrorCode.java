/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oracle.helidon.oci.errorcode;

import java.util.Objects;

import io.helidon.http.Status;

import static io.helidon.http.Status.BAD_REQUEST_400;
import static io.helidon.http.Status.CONFLICT_409;
import static io.helidon.http.Status.FORBIDDEN_403;
import static io.helidon.http.Status.INTERNAL_SERVER_ERROR_500;
import static io.helidon.http.Status.NOT_FOUND_404;
import static io.helidon.http.Status.PRECONDITION_FAILED_412;
import static io.helidon.http.Status.SERVICE_UNAVAILABLE_503;
import static io.helidon.http.Status.TOO_MANY_REQUESTS_429;
import static io.helidon.http.Status.UNAUTHORIZED_401;
import static io.helidon.http.Status.UNPROCESSABLE_CONTENT_422;

/**
 * Error codes that can be used with {@link com.oracle.helidon.oci.errorcode.RenderableException}.
 * <p>
 * Documentation for each value provides description of the error, and the Status code in parentheses.
 */
public enum ErrorCode {
    /*
    Based on {@code com.oracle.pic.commons.exceptions.server.ErrorCode} but without
    depending on jakarta packages.
     */
    /**
     * Incorrectly formatted request (Bad Request).
     */
    CannotParseRequest(BAD_REQUEST_400, "Incorrectly formatted request. Please refer to our documentation for help."),
    /**
     * Invalid parameter (Bad Request).
     */
    InvalidParameter(BAD_REQUEST_400, "Invalid Parameter"),
    /**
     * Missing parameter (Bad Request).
     */
    MissingParameter(BAD_REQUEST_400, "Missing Parameter"),
    /**
     * Quota for compartment exceeded (Bad Request).
     */
    QuotaExceeded(BAD_REQUEST_400, "The quota for this compartment has been exceeded."),
    /**
     * Limit for tenancy exceeded (Bad Request).
     */
    LimitExceeded(BAD_REQUEST_400, "The limit for this tenancy has been exceeded."),
    /**
     * Resource creation is disabled (Bad Request).
     */
    ResourceDisabled(BAD_REQUEST_400, "Resource creation is disabled."),
    /**
     * Authorization failed or resource not found (Bad Request).
     */
    RelatedResourceNotAuthorizedOrNotFound(BAD_REQUEST_400, "Authorization failed or related resource not found."),
    /**
     * Out of capacity in this AD (Bad Request).
     */
    SeOutOfCapacity(BAD_REQUEST_400,
                    "We are out of capacity in this AD based on your fault constraints or specific request."),
    /**
     * Not authenticated (Unauthorized).
     */
    NotAuthenticated(UNAUTHORIZED_401,
                     "The required information to complete authentication was not provided or was incorrect."),
    /**
     * Not allowed (Forbidden).
     */
    NotAllowed(FORBIDDEN_403, "This operation must be directed at the home region."),
    /**
     * Not authorized or not found (Not Found).
     */
    NotAuthorizedOrNotFound(NOT_FOUND_404, "Authorization failed or requested resource not found."),
    /**
     * Pagination token expired (Bad Request).
     */
    PaginationTokenExpired(BAD_REQUEST_400, "Pagination token is too old to be used with current state data source."),
    /**
     * Wrong pagination token format (Bad Request).
     */
    PaginationTokenFormat(BAD_REQUEST_400, "Wrong pagination token format."),
    /**
     * No E-Tag match (Precondition Failed).
     */
    NoEtagMatch(PRECONDITION_FAILED_412),
    /**
     * Conflict (Conflict).
     */
    Conflict(CONFLICT_409),
    /**
     * Not authorized or resource exists (Conflict).
     */
    NotAuthorizedOrResourceAlreadyExists(CONFLICT_409, "Authorization failed or requested resource already exists."),
    /**
     * Bad retry token (Conflict).
     */
    InvalidatedRetryToken(CONFLICT_409,
                          "The retry token you provided was used in an earlier request that resulted in a system update, but a "
                                  + "subsequent operation has invalidated the token. This can happen, for example, in cases "
                                  + "where an entity created with the same token has since been deleted. If the system state "
                                  + "change associated with this request should be performed again, then retry with a different"
                                  + " retry token."),
    /**
     * Incorrect state (Conflict).
     */
    IncorrectState(CONFLICT_409, "Incorrect State"),
    /**
     * Resource locked (Conflict).
     */
    ResourceLocked(CONFLICT_409, "Resource has been locked."),
    /**
     * External server unreachable (Service Unavailable).
     */
    ExternalServerUnreachable(SERVICE_UNAVAILABLE_503,
                              "A connection with an external system needed to fulfill the " +
                                      "request could not be established."),
    /**
     * External server timeout (Service Unavailable).
     */
    ExternalServerTimeout(SERVICE_UNAVAILABLE_503,
                          "A connection with an external system needed to fulfill the " +
                                  "request timed out before a response was received."),
    /**
     * External server invalid response (Service Unavailable).
     */
    ExternalServerInvalidResponse(SERVICE_UNAVAILABLE_503,
                                  "A connection with an external system needed to fulfill the " +
                                          "request resulted in an unacceptable response."),
    /**
     * Unprocessable entity (Unprocessable Content).
     */
    UnprocessableEntity(UNPROCESSABLE_CONTENT_422, "Unprocessable Entity"),
    /**
     * Internal server error (Internal Server Error).
     */
    InternalError(INTERNAL_SERVER_ERROR_500),
    /**
     * User-rate limit exceeded (Too Many Requests).
     */
    TooManyRequests(TOO_MANY_REQUESTS_429, "User-rate limit exceeded.");

    private final Status status;

    ErrorCode(Status status) {
        this(status, status.reasonPhrase());
    }

    ErrorCode(Status status, String reasonPhrase) {
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(reasonPhrase, "Reason phrase cannot be null");

        this.status = Status.create(status.code(), reasonPhrase);
    }

    /**
     * The HTTP status with reason phrase.
     *
     * @return status
     */
    public Status status() {
        return this.status;
    }
}
