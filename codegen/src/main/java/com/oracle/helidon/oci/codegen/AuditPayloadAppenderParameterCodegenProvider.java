/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.NoSuchElementException;

import io.helidon.common.types.TypeName;
import io.helidon.declarative.codegen.http.webserver.ParameterCodegenContext;
import io.helidon.declarative.codegen.http.webserver.spi.HttpParameterCodegenProvider;

/**
 * Code generation support for direct {@code AuditPayloadAppender} endpoint parameters.
 */
public class AuditPayloadAppenderParameterCodegenProvider implements HttpParameterCodegenProvider {

    private static final TypeName AUDIT_PAYLOAD_APPENDER =
            TypeName.create("com.oracle.pic.sherlock.collector.AuditPayloadAppender");

    @Override
    public boolean codegen(ParameterCodegenContext ctx) {
        if (!ctx.annotations().isEmpty()) {
            return false;
        }
        if (!AUDIT_PAYLOAD_APPENDER.equals(ctx.parameterType())) {
            return false;
        }

        ctx.contentBuilder()
                .addContentLine(ctx.serverRequestParamName())
                .increaseContentPadding()
                .increaseContentPadding()
                .addContentLine(".context()")
                .addContent(".get(")
                .addContent(AUDIT_PAYLOAD_APPENDER)
                .addContent(".class.getName(), ")
                .addContent(AUDIT_PAYLOAD_APPENDER)
                .addContentLine(".class)")
                .addContent(".orElseThrow(() -> new ")
                .addContent(NoSuchElementException.class)
                .addContent("(\"")
                .addContent(AUDIT_PAYLOAD_APPENDER)
                .addContent(" is not present in request context, required for parameter ")
                .addContent(ctx.paramName())
                .addContentLine("\"));")
                .decreaseContentPadding()
                .decreaseContentPadding();
        return true;
    }
}
