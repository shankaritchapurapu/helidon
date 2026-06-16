/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.NoSuchElementException;
import java.util.Optional;

import io.helidon.webserver.http.ServerRequest;

import com.oracle.pic.sherlock.collector.AuditPayloadAppender;

/**
 * Accessors for the current request's audit payload.
 */
final class AuditPayloadAppenders {
    private static final String APPENDER_ATTRIBUTE_NAME = AuditPayloadAppender.class.getName();

    private AuditPayloadAppenders() {
    }

    /**
     * Returns the live audit payload appender registered for this request, if audit processing is active.
     *
     * @param request server request
     * @return current audit payload appender
     */
    public static Optional<AuditPayloadAppender> current(ServerRequest request) {
        return request.context().get(APPENDER_ATTRIBUTE_NAME, AuditPayloadAppender.class);
    }

    /**
     * Returns the live audit payload appender registered for this request.
     *
     * @param request server request
     * @return current audit payload appender
     * @throws NoSuchElementException if audit processing is not active for this request
     */
    public static AuditPayloadAppender require(ServerRequest request) {
        return current(request)
                .orElseThrow(() -> new NoSuchElementException("AuditPayloadAppender is not present in request context"));
    }
}
