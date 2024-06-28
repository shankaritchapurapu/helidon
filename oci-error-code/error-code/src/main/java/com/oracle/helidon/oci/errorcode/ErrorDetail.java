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
package com.oracle.helidon.oci.errorcode;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Rendered type for error detail that supports both JSON-Binding and Jackson.
 * This class is not intended for direct use, use {@link com.oracle.helidon.oci.errorcode.RenderableException}
 * instead.
 */
public class ErrorDetail {
    /*
    This class uses Java beans style getters, to be supported both by
    Jackson and JSON-B.
     */
    private static final String CODE = "code";
    private static final String ORIGINAL_MESSAGE = "originalMessage";
    private static final String ORIGINAL_MESSAGE_TEMPLATE = "originalMessageTemplate";
    private static final String MESSAGE_ARGUMENTS = "messageArguments";
    private static final String MESSAGE = "message";

    private final ErrorCode errorCode;
    private final String originalMessage;
    private final String originalMessageTemplate;
    private final Map<String, String> messageArguments;
    private final String message;

    private ErrorDetail(ErrorCode errorCode,
                        String message,
                        String originalMessage,
                        String originalMessageTemplate,
                        Map<String, String> messageArguments) {

        Objects.requireNonNull(errorCode, "Error code must not be null");
        Objects.requireNonNull(message, "Message must not be null");

        this.errorCode = errorCode;
        this.message = message;
        this.originalMessage = originalMessage;
        this.originalMessageTemplate = originalMessageTemplate;
        this.messageArguments = messageArguments;
    }

    /**
     * Create a new instance.
     * This factory method accepts nulls, as it is used by JSON deserializers.
     *
     * @param code                    error code (required)
     * @param message                 error message (required)
     * @param originalMessage         original message (nullable)
     * @param originalMessageTemplate original message template (nullable)
     * @param messageArguments        message arguments (nullable)
     */
    @JsonbCreator
    @JsonCreator
    public static ErrorDetail create(@JsonbProperty(CODE)
                                     @JsonProperty(CODE)
                                     ErrorCode code,
                                     @JsonbProperty(MESSAGE)
                                     @JsonProperty(MESSAGE)
                                     String message,
                                     @JsonbProperty(ORIGINAL_MESSAGE)
                                     @JsonProperty(ORIGINAL_MESSAGE)
                                     String originalMessage,
                                     @JsonbProperty(ORIGINAL_MESSAGE_TEMPLATE)
                                     @JsonProperty(ORIGINAL_MESSAGE_TEMPLATE)
                                     String originalMessageTemplate,
                                     @JsonbProperty(MESSAGE_ARGUMENTS) @JsonProperty(MESSAGE_ARGUMENTS)
                                     Map<String, String> messageArguments) {
        return new ErrorDetail(code, message, originalMessage, originalMessageTemplate, messageArguments);
    }

    /**
     * Error code.
     *
     * @return error code
     */
    @JsonProperty(CODE)
    @JsonbProperty(CODE)
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Original message.
     * May be null (this method is used by JSON serializers).
     *
     * @return original message
     */
    @JsonProperty(ORIGINAL_MESSAGE)
    @JsonbProperty(ORIGINAL_MESSAGE)
    public String getOriginalMessage() {
        return originalMessage;
    }

    /**
     * Original message template.
     * May be null (this method is used by JSON serializers).
     *
     * @return original message template
     */
    @JsonProperty(ORIGINAL_MESSAGE_TEMPLATE)
    @JsonbProperty(ORIGINAL_MESSAGE_TEMPLATE)
    public String getOriginalMessageTemplate() {
        return originalMessageTemplate;
    }

    /**
     * Message arguments.
     * May be null (this method is used by JSON deserializers).
     *
     * @return message arguments
     */
    @JsonProperty(MESSAGE_ARGUMENTS)
    @JsonbProperty(MESSAGE_ARGUMENTS)
    public Map<String, String> getMessageArguments() {
        return messageArguments;
    }

    /**
     * Error message.
     *
     * @return error message
     */
    @JsonProperty(MESSAGE)
    @JsonbProperty(MESSAGE)
    public String getMessage() {
        return message;
    }
}
