/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import java.util.Map;

import io.helidon.json.binding.Json;

/**
 * Small JSON-friendly OCI-style error payload used by the example endpoint.
 *
 * @param code OCI error code
 * @param message rendered error message
 * @param originalMessage optional original message
 * @param originalMessageTemplate optional original message template
 * @param messageArguments optional message arguments
 */
@Json.Entity
record OciErrorResponse(String code,
                        String message,
                        String originalMessage,
                        String originalMessageTemplate,
                        Map<String, String> messageArguments) {
}
