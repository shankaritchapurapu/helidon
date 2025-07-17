/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Applications can throw instances of this class and expect a response
 * to be rendered according to the OCI error code rules.
 * <p>
 * Note that the exception itself is not renderable, you need to use {@link #errorDetail()}
 * to obtain the JSON serializable object.
 * <p>
 * There are two mapper modules - one for MicroProfile based project (with exception mapper for
 * JAX-RS, and a mapper for MP REST client), and another one for Helidon WebServer (with error handler).
 * It is sufficient to add these modules to your classpath, mappers are discovered through service loader.
 */
public final class RenderableException extends RuntimeException {
    /*
     * Similar to {@code com.oracle.pic.commons.exceptions.server.RenderableException}
     * but without depending on jakarta packages.
     */

    private final ErrorDetail errorDetail;
    private final ErrorCode errorCode;

    /**
     * Create an exception with required information.
     * This will use the default message associated with the error code provided.
     *
     * @param code error code, must not be null
     */
    public RenderableException(ErrorCode code) {
        this(code, code.errorMessage());
    }

    /**
     * Create an exception with custom error message.
     *
     * @param code    error code, must not be null
     * @param message message, must not be null
     */
    public RenderableException(ErrorCode code,
                               String message) {
        super(message);

        Objects.requireNonNull(code, "Error code must not be null");
        Objects.requireNonNull(message, "Message must not be null");

        this.errorCode = code;
        this.errorDetail = ErrorDetail.create(code.errorCode(),
                                              message,
                                              null,
                                              null,
                                              null);
    }

    /**
     * Create an exception with required information and a cause.
     *
     * @param code  error code, must not be null
     * @param cause cause of this exception
     */
    public RenderableException(ErrorCode code,
                               Throwable cause) {
        this(code, code.errorMessage(), cause);
    }

    /**
     * Create an exception with custom error message and a cause.
     *
     * @param code    error code, must not be null
     * @param message message, must not be null
     * @param cause   cause of this exception
     */
    public RenderableException(ErrorCode code,
                               String message,
                               Throwable cause) {
        super(message, cause);

        Objects.requireNonNull(code, "Error code must not be null");
        Objects.requireNonNull(message, "Message must not be null");

        this.errorCode = code;
        this.errorDetail = ErrorDetail.create(code.errorCode(),
                                              message,
                                              null,
                                              null,
                                              null);
    }

    /**
     * Create a new exception with an error code, format string, and format arguments.
     *
     * @param code         error code
     * @param formatString format string used with {@link java.lang.String#format(String, Object...)}
     * @param args         arguments of the format
     */
    public RenderableException(ErrorCode code, String formatString, Object... args) {
        super(computeMessage(formatString, args), computeCause(args));

        Objects.requireNonNull(code, "Error code must not be null");
        Objects.requireNonNull(formatString, "Format string must not be null");
        Objects.requireNonNull(args, "Format arguments must not be null");

        this.errorCode = code;
        this.errorDetail = ErrorDetail.create(code.errorCode(),
                                              super.getMessage(),
                                              null,
                                              null,
                                              null);
    }

    /**
     * Create an exception with all information.
     *
     * @param code                    error code, must not be null
     * @param message                 message, must not be null
     * @param originalMessage         original message (nullable)
     * @param originalMessageTemplate original template (nullable)
     * @param messageArguments        message arguments (nullable)
     */
    public RenderableException(ErrorCode code,
                               String message,
                               String originalMessage,
                               String originalMessageTemplate,
                               Map<String, String> messageArguments) {
        super(message);

        Objects.requireNonNull(code, "Error code must not be null");
        Objects.requireNonNull(message, "Message must not be null");

        this.errorCode = code;
        this.errorDetail = ErrorDetail.create(code.errorCode(),
                                              message,
                                              originalMessage,
                                              originalMessageTemplate,
                                              messageArguments);
    }

    /**
     * Create an exception with all information.
     *
     * @param code                    error code, must not be null
     * @param message                 message, must not be null
     * @param originalMessage         original message (nullable)
     * @param originalMessageTemplate original template (nullable)
     * @param messageArguments        message arguments (nullable)
     * @param cause                   original cause
     */
    public RenderableException(ErrorCode code,
                               String message,
                               String originalMessage,
                               String originalMessageTemplate,
                               Map<String, String> messageArguments,
                               Throwable cause) {
        super(message, cause);

        Objects.requireNonNull(code, "Error code must not be null");
        Objects.requireNonNull(message, "Message must not be null");

        this.errorCode = code;
        this.errorDetail = ErrorDetail.create(code.errorCode(),
                                              message,
                                              originalMessage,
                                              originalMessageTemplate,
                                              messageArguments);
    }

    /**
     * A JSON-Serializable object with error details.
     *
     * @return error detail for serialization
     */
    public ErrorDetail errorDetail() {
        return errorDetail;
    }

    /**
     * Error code enum constant.
     *
     * @return error code
     */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * Original message.
     *
     * @return original message if defined
     */
    public Optional<String> originalMessage() {
        return Optional.ofNullable(errorDetail.getOriginalMessage());
    }

    /**
     * Original message template.
     *
     * @return original message template if defined.
     */
    public Optional<String> originalMessageTemplate() {
        return Optional.ofNullable(errorDetail.getOriginalMessageTemplate());
    }

    /**
     * Message arguments, or empty map.
     *
     * @return message arguments
     */
    public Map<String, String> messageArguments() {
        return Optional.ofNullable(errorDetail.getMessageArguments()).orElseGet(Map::of);
    }

    @Override
    public String toString() {
        return "RenderableException(errorCode=" + errorCode()
                + ", originalMessage=" + originalMessage()
                + ", originalMessageTemplate=" + originalMessageTemplate()
                + ", messageArguments=" + messageArguments() + ")";
    }

    private static String computeMessage(String formatString, Object... args) {
        if (args.length == 0) {
            return formatString;
        } else {
            Object last = args[args.length - 1];
            return last instanceof Throwable
                    ? String.format(formatString,
                                    Arrays.copyOf(args, args.length - 1))
                    : String.format(formatString, args);
        }
    }

    private static Throwable computeCause(Object... args) {
        if (args.length == 0) {
            return null;
        } else {
            Object last = args[args.length - 1];
            return last instanceof Throwable ? (Throwable) last : null;
        }
    }
}
