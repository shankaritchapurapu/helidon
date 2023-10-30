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

import javax.ws.rs.Priorities;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Maps a Response to a {@link RenderableException}.
 */
public class ErrorCodeResponseMapper implements ResponseExceptionMapper<RenderableException> {

    /**
     * Attempt to convert any response with a code >= 400 to a {@link RenderableException}.
     *
     * @param response the response to convert
     * @return the exception
     * @throws ProcessingException if a problem is encountered reading the entity
     * @throws IllegalStateException if called after the response has been closed
     */
    @Override
    public RenderableException toThrowable(Response response) {
        // we need to buffer the entity for other mappers
        if (!response.bufferEntity()) {
            throw new ProcessingException("Unable to buffer response entity");
        }

        ErrorDetail errorDetail = response.readEntity(ErrorDetail.class);
        return new RenderableException(null,
                errorDetail.getErrorCode(),
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
