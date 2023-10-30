/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.integrations.oci.errorcode;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Maps a {@link RenderableException} to a response with JSON payload as described in
 * <a href="https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Error+Codes">
 * Error Codes</a>.
 */
public class ErrorCodeExceptionMapper implements ExceptionMapper<RenderableException> {

    @Override
    public Response toResponse(RenderableException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return Response.status(errorCode.getStatusType().getStatusCode())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(exception.getErrorDetail())
                .build();
    }
}
