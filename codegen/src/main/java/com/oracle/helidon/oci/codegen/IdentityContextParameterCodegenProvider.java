/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.NoSuchElementException;

import io.helidon.common.types.TypeName;
import io.helidon.declarative.codegen.http.webserver.ParameterCodegenContext;
import io.helidon.declarative.codegen.http.webserver.spi.HttpParameterCodegenProvider;

/**
 * Code generation support for direct {@code IdentityContext} endpoint parameters.
 */
public class IdentityContextParameterCodegenProvider implements HttpParameterCodegenProvider {

    private static final TypeName IDENTITY_CONTEXT = TypeName.create("com.oracle.helidon.oci.identity.IdentityContext");

    @Override
    public boolean codegen(ParameterCodegenContext ctx) {
        if (!ctx.annotations().isEmpty()) {
            return false;
        }
        if (!IDENTITY_CONTEXT.equals(ctx.parameterType())) {
            return false;
        }

        // IdentityContext is request-scoped and is registered by the authorization interceptor.
        ctx.contentBuilder()
                .addContentLine(ctx.serverRequestParamName())
                .increaseContentPadding()
                .increaseContentPadding()
                .addContentLine(".context()")
                .addContent(".get(")
                .addContent(IDENTITY_CONTEXT)
                .addContentLine(".class)")
                .addContent(".orElseThrow(() -> new ")
                .addContent(NoSuchElementException.class)
                .addContent("(\"")
                .addContent(IDENTITY_CONTEXT)
                .addContent(" is not present in request context, required for parameter ")
                .addContent(ctx.paramName())
                .addContentLine("\"));")
                .decreaseContentPadding()
                .decreaseContentPadding();
        return true;
    }
}
