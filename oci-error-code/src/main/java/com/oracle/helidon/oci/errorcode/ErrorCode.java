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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;

/**
 * Based on {@code com.oracle.pic.commons.exceptions.server.ErrorCode} but without
 * depending on jakarta packages.
 */
public enum ErrorCode {

    CannotParseRequest(Status.BAD_REQUEST, "Incorrectly formatted request. Please refer to our documentation for help."),
    InvalidParameter(Status.BAD_REQUEST, "Invalid Parameter"),
    MissingParameter(Status.BAD_REQUEST, "Missing Parameter"),
    QuotaExceeded(Status.BAD_REQUEST, "The quota for this compartment has been exceeded."),
    LimitExceeded(Status.BAD_REQUEST, "The limit for this tenancy has been exceeded."),
    ResourceDisabled(Status.BAD_REQUEST, "Resource creation is disabled."),
    RelatedResourceNotAuthorizedOrNotFound(Status.BAD_REQUEST, "Authorization failed or related resource not found."),
    SeOutOfCapacity(Status.BAD_REQUEST, "We are out of capacity in this AD based on your fault constraints or specific request."),
    NotAuthenticated(Status.UNAUTHORIZED, "The required information to complete authentication was not provided or was incorrect."),
    NotAllowed(Status.FORBIDDEN, "This operation must be directed at the home region."),
    NotAuthorizedOrNotFound(Status.NOT_FOUND, "Authorization failed or requested resource not found."),
    PaginationTokenExpired(Status.BAD_REQUEST, "Pagination token is too old to be used with current state data source."),
    PaginationTokenFormat(Status.BAD_REQUEST, "Wrong pagination token format."),
    NoEtagMatch(Status.PRECONDITION_FAILED),
    Conflict(Status.CONFLICT),
    NotAuthorizedOrResourceAlreadyExists(Status.CONFLICT, "Authorization failed or requested resource already exists."),
    InvalidatedRetryToken(Status.CONFLICT, "The retry token you provided was used in an earlier request that resulted in a system update, but a subsequent operation has invalidated the token. This can happen, for example, in cases where an entity created with the same token has since been deleted. If the system state change associated with this request should be performed again, then retry with a different retry token."),
    IncorrectState(Status.CONFLICT, "Incorrect State"),
    ResourceLocked(Status.CONFLICT, "Resource has been locked."),
    ExternalServerUnreachable(Status.SERVICE_UNAVAILABLE,
            "A connection with an external system needed to fulfill the " +
                    "request could not be established."),
    ExternalServerTimeout(Status.SERVICE_UNAVAILABLE,
            "A connection with an external system needed to fulfill the " +
                    "request timed out before a response was received."),
    ExternalServerInvalidResponse(Status.SERVICE_UNAVAILABLE,
            "A connection with an external system needed to fulfill the " +
                    "request resulted in an unacceptable response."),

    UnprocessableEntity(new Response.StatusType() {
        public int getStatusCode() {
            return 422;
        }

        public Response.Status.Family getFamily() {
            return Family.CLIENT_ERROR;
        }

        public String getReasonPhrase() {
            return "Unprocessable Entity";
        }
    }),
    InternalError(Status.INTERNAL_SERVER_ERROR),

    TooManyRequests(new Response.StatusType() {
        public int getStatusCode() {
            return 429;
        }

        public Response.Status.Family getFamily() {
            return Family.CLIENT_ERROR;
        }

        public String getReasonPhrase() {
            return "User-rate limit exceeded.";
        }
    }),

    /** @deprecated */
    @Deprecated
    NotAcceptable(Status.NOT_ACCEPTABLE),
    /** @deprecated */
    @Deprecated
    MethodNotAllowed(Status.METHOD_NOT_ALLOWED),
    /** @deprecated */
    @Deprecated
    LengthRequired(Status.LENGTH_REQUIRED),
    /** @deprecated */
    @Deprecated
    PreconditionFailed(Status.PRECONDITION_FAILED),
    /** @deprecated */
    @Deprecated
    RequestEntityTooLarge(Status.REQUEST_ENTITY_TOO_LARGE),
    /** @deprecated */
    @Deprecated
    UnsupportedMediaType(Status.UNSUPPORTED_MEDIA_TYPE),
    /** @deprecated */
    @Deprecated
    Forbidden(Status.FORBIDDEN),
    /** @deprecated */
    @Deprecated
    NotFound(Status.NOT_FOUND),
    /** @deprecated */
    @Deprecated
    NotImplemented(Status.NOT_IMPLEMENTED),
    /** @deprecated */
    @Deprecated
    ServiceUnavailable(Status.SERVICE_UNAVAILABLE);

    private final Response.StatusType statusType;
    private final String defaultMessage;

    /** @deprecated */
    @Deprecated
    public Response.Status getStatus() {
        return (Response.Status) this.statusType;
    }

    private ErrorCode(Response.Status status) {
        this(status, status.getReasonPhrase());
    }

    private ErrorCode(Response.StatusType statusType) {
        this(statusType, statusType.getReasonPhrase());
    }

    private ErrorCode(Response.StatusType statusType, String defaultMessage) {
        if (statusType == null) {
            throw new NullPointerException("statusType is marked non-null but is null");
        } else if (defaultMessage == null) {
            throw new NullPointerException("defaultMessage is marked non-null but is null");
        } else {
            this.statusType = statusType;
            this.defaultMessage = defaultMessage;
        }
    }
     
    public Response.StatusType getStatusType() {
        return this.statusType;
    }

    public String getDefaultMessage() {
        return this.defaultMessage;
    }
}
