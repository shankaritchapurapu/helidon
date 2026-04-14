/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.NoSuchElementException;

import io.helidon.common.types.TypeName;
import io.helidon.declarative.codegen.http.webserver.ParameterCodegenContext;
import io.helidon.declarative.codegen.http.webserver.spi.HttpParameterCodegenProvider;

/**
 * Code generation support for direct {@code OciRequestId} endpoint parameters.
 */
public class OciRequestIdParameterCodegenProvider implements HttpParameterCodegenProvider {

    private static final TypeName OCI_REQUEST_ID = TypeName.create("com.oracle.helidon.oci.requestid.OciRequestId");

    @Override
    public boolean codegen(ParameterCodegenContext ctx) {
        if (!ctx.annotations().isEmpty()) {
            return false;
        }
        if (!OCI_REQUEST_ID.equals(ctx.parameterType())) {
            return false;
        }

        // OciRequestId is request-scoped and is registered by RequestIdServerFilter.
        ctx.contentBuilder()
                .addContentLine(ctx.serverRequestParamName())
                .increaseContentPadding()
                .increaseContentPadding()
                .addContentLine(".context()")
                .addContent(".get(")
                .addContent(OCI_REQUEST_ID)
                .addContentLine(".class)")
                .addContent(".orElseThrow(() -> new ")
                .addContent(NoSuchElementException.class)
                .addContent("(\"")
                .addContent(OCI_REQUEST_ID)
                .addContent(" is not present in request context, required for parameter ")
                .addContent(ctx.paramName())
                .addContentLine("\"));")
                .decreaseContentPadding()
                .decreaseContentPadding();
        return true;
    }
}
