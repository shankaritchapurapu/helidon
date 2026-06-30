/*
 * Copyright (c) 2023, 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.requestid;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.pic.commons.rid.RequestIdUtils;

import static com.oracle.pic.commons.rid.RequestIdUtils.generateUniqueId;

record OciRequestIdImpl(String customerId, String traceId, String spanId) implements OciRequestId {
    static final String DELIMITER = RequestIdUtils.DELIMITER;

    private static final System.Logger LOGGER = System.getLogger(OciRequestId.class.getName());
    private static final int MAX_PART_LENGTH = RequestIdUtils.MAX_CID_LENGTH;
    private static final boolean[] ALLOWED_CHARS = new boolean[0x7A + 1]; // highest char

    static {
        // https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Request+IDs
        ALLOWED_CHARS[0x5F] = true; // underscore
        ALLOWED_CHARS[0x2D] = true; // dash

        // ascii alphanumeric
        for (int i = 0x30; i <= 0x39; i++) {
            // 0x30 is Zero
            // 0x39 is Nine
            ALLOWED_CHARS[i] = true;
        }
        for (int i = 0x41; i <= 0x5A; i++) {
            // 0x41 is A
            // 0x5A is Z
            ALLOWED_CHARS[i] = true;
        }
        for (int i = 0x61; i <= 0x7A; i++) {
            // 0x61 is a
            // 0x7A is z
            ALLOWED_CHARS[i] = true;
        }
        // if you need to add any char above 0x7A, increase the array size
    }

    static OciRequestId parseUpstreamRequest(String headerValue) {
        Objects.requireNonNull(headerValue);

        String[] parts = headerValue.split(DELIMITER, -1);

        if (parts.length == 1) {
            // only customer id (maybe empty)
            return new OciRequestIdImpl(clean(headerValue), generateUniqueId(), generateUniqueId());
        }
        if (parts.length == 3) {
            // this is invalid, span id should never be sent from upstream services
            warnInvalid("Received upstream request id with spanId.", parts);
        } else if (parts.length > 3) {
            // this is invalid, wrong number of parts
            warnInvalid("Received upstream request id with too many elements: " + parts.length + ".", parts);
        }

        String traceId = clean(parts[1]);
        if (traceId.isEmpty()) {
            warnInvalid("Received upstream request id with empty traceId.", parts);
            traceId = generateUniqueId();
        }
        return new OciRequestIdImpl(clean(parts[0]), traceId, generateUniqueId());
    }

    static OciRequestId parseDownstreamResponse(OciRequestId requestId, String headerValue) {
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(headerValue);

        String[] parts = headerValue.split(DELIMITER);

        if (parts.length == 3) {
            // the correct response
            return new OciRequestIdImpl(clean(parts[0]),
                                        clean(parts[1]),
                                        clean(parts[2]));
        }
        warnInvalid("Downstream response with invalid request id, less than 3 parts found.", headerValue);

        return new OciRequestIdImpl(requestId.customerId(), requestId.traceId(), "");
    }

    static OciRequestId generate() {
        return new OciRequestIdImpl("", generateUniqueId(), generateUniqueId());
    }

    static String stripNotAllowedCharacters(String part) {
        StringBuilder sb = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (c < ALLOWED_CHARS.length && ALLOWED_CHARS[c]) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String clean(String part) {
        String cleaned = stripNotAllowedCharacters(part);
        if (cleaned.length() != part.length()) {
            warnInvalid("Request id contains invalid ASCII characters, removed.", cleaned);
        }
        if (part.length() > MAX_PART_LENGTH) {
            warnInvalid("Request id contains too long part, truncated.", cleaned);
        }

        cleaned = shorten(cleaned, MAX_PART_LENGTH);

        return cleaned;
    }

    private static String shorten(String value, int maxLength) {
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }

    private static void warnInvalid(String message, String headerValue) {
        // this is a requirement by the OCI specification of request ids
        // the value must first be cleaned, as this may be actual user input
        // max length is three ids + two separators
        String cleaned = shorten(stripNotAllowedCharacters(headerValue), MAX_PART_LENGTH * 3 + 2);

        LOGGER.log(Level.WARNING, message + " Cleaned and shortened value: {0}", stripNotAllowedCharacters(cleaned));
    }

    private static void warnInvalid(String message, String[] parts) {
        // this is a requirement by the OCI specification of request ids
        // the value must first be cleaned, as this may be actual user input
        // max length is three ids + two separators
        String stripped = Stream.of(parts)
                .map(OciRequestIdImpl::stripNotAllowedCharacters)
                .collect(Collectors.joining("/"));
        String cleaned = shorten(stripped, MAX_PART_LENGTH * 3 + 2);

        LOGGER.log(Level.WARNING, message + " Cleaned and shortened value: {0}", cleaned);
    }
}
