/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Rendered type for error detail that supports both JSON-Binding and Jackson.
 * This class is only intended to read entity from a server response (when you act as a client).
 * To correctly handle server response (when you act as a server),
 * throw {@link RenderableException}.
 *
 * @see ErrorCode
 * @see ErrorCodes
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

    private final String errorCode;
    private final String originalMessage;
    private final String originalMessageTemplate;
    private final Map<String, String> messageArguments;
    private final String message;

    private ErrorDetail(String errorCode,
                        String message,
                        String originalMessage,
                        String originalMessageTemplate,
                        Map<String, String> messageArguments) {

        this.errorCode = errorCode;
        this.message = message;
        this.originalMessage = originalMessage;
        this.originalMessageTemplate = originalMessageTemplate;
        this.messageArguments = messageArguments;
    }

    /**
     * Create an error detail for a specific error code, with default message.
     *
     * @param errorCode error code
     * @return error detail for the provided code
     */
    public static ErrorDetail create(ErrorCode errorCode) {
        return new ErrorDetail(errorCode.errorCode(),
                               errorCode.errorMessage(),
                               null,
                               null,
                               null);
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
                                         String code,
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
        Objects.requireNonNull(code, "Error code must not be null");
        Objects.requireNonNull(message, "Message must not be null");

        return new ErrorDetail(code, message, originalMessage, originalMessageTemplate, messageArguments);
    }

    /**
     * Error code.
     *
     * @return error code
     * @see ErrorCode#create(io.helidon.http.Status, ErrorDetail)
     */
    @JsonProperty(CODE)
    @JsonbProperty(CODE)
    public String getErrorCode() {
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
