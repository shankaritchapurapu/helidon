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
package com.oracle.helidon.integrations.oci.errorcode;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Based on {@code com.oracle.pic.commons.exceptions.server.RenderableException} but without
 * depending on javax packages.
 */
public class ErrorDetail {

    @JsonProperty("code")
    private final ErrorCode errorCode;

    @JsonProperty("originalMessage")
    private final String originalMessage;

    @JsonProperty("originalMessageTemplate")
    private final String originalMessageTemplate;

    @JsonProperty("messageArguments")
    private final Map<String, String> messageArguments;

    @JsonProperty("message")
    private final String message;

    private ErrorDetail() {
        this.errorCode = null;
        this.message = null;
        this.originalMessage = null;
        this.originalMessageTemplate = null;
        this.messageArguments = null;
    }

    public ErrorDetail(ErrorCode code, String message) {
        this.errorCode = code;
        this.message = message;
        this.originalMessage = null;
        this.originalMessageTemplate = null;
        this.messageArguments = null;
    }

    public ErrorDetail(ErrorCode code,
                       String message,
                       String originalMessage,
                       String originalMessageTemplate,
                       Map<String, String> messageArguments) {
        this.errorCode = code;
        this.message = message;
        this.originalMessage = originalMessage;
        this.originalMessageTemplate = originalMessageTemplate;
        this.messageArguments = messageArguments;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public String getOriginalMessageTemplate() {
        return originalMessageTemplate;
    }

    public Map<String, String> getMessageArguments() {
        return messageArguments;
    }

    public String getMessage() {
        return message;
    }
}
