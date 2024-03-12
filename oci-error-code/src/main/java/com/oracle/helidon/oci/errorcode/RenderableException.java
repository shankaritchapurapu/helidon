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

import java.util.Arrays;
import java.util.Map;

/**
 * Applications can throw instances of this class and expect a response
 * to be rendered according othe OCI error code rules by including this
 * module.
 * <p>
 * Similar to {@code com.oracle.pic.commons.exceptions.server.RenderableException}
 * but without depending on jakarta packages.
 */
public final class RenderableException extends RuntimeException {

    private final ErrorDetail errorDetail;

    public RenderableException(ErrorCode code, String formatString, Object... args) {
        super(computeMessage(formatString, args), computeCause(args));
        this.errorDetail = new ErrorDetail(code, super.getMessage());
    }

    public RenderableException(Throwable cause, ErrorCode code, String message, String originalMessage,
                               String originalMessageTemplate, Map<String, String> messageArguments) {
        super(message, cause);
        this.errorDetail = new ErrorDetail(code, message, originalMessage,
                originalMessageTemplate, messageArguments);
    }

    private static String computeMessage(String formatString, Object... args) {
        if (args.length == 0) {
            return formatString;
        } else {
            Object last = args[args.length - 1];
            return last instanceof Throwable ? String.format(formatString,
                    Arrays.copyOf(args, args.length - 1)) : String.format(formatString, args);
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

    public ErrorDetail getErrorDetail() {
        return errorDetail;
    }

    public ErrorCode getErrorCode() {
        return errorDetail.getErrorCode();
    }

    public String getOriginalMessage() {
        return errorDetail.getOriginalMessage();
    }

    public String getOriginalMessageTemplate() {
        return errorDetail.getOriginalMessageTemplate();
    }

    public Map<String, String> getMessageArguments() {
        return errorDetail.getMessageArguments();
    }

    public String toString() {
        return "RenderableException(errorCode=" + getErrorCode()
                + ", originalMessage=" + getOriginalMessage()
                + ", originalMessageTemplate=" + getOriginalMessageTemplate()
                + ", messageArguments=" + getMessageArguments() + ")";
    }
}
