/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.NoSuchElementException;
import java.util.Optional;

import io.helidon.common.types.TypeName;
import io.helidon.declarative.codegen.http.webserver.ParameterCodegenContext;
import io.helidon.declarative.codegen.http.webserver.spi.HttpParameterCodegenProvider;

/**
 * Code generation support for direct OCI Identity endpoint parameters.
 */
public class IdentityContextParameterCodegenProvider implements HttpParameterCodegenProvider {

    private static final TypeName IDENTITY_CONTEXT = TypeName.create("com.oracle.helidon.oci.identity.IdentityContext");
    private static final TypeName PRINCIPAL = TypeName.create("com.oracle.pic.identity.authentication.Principal");
    private static final TypeName AUTHORIZATION_REQUEST =
            TypeName.create("com.oracle.pic.identity.authorization.sdk.AuthorizationRequest");
    private static final TypeName AUTH_CONTEXT_REQUEST_FILTER =
            TypeName.create("com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter");

    @Override
    public boolean codegen(ParameterCodegenContext ctx) {
        if (!ctx.annotations().isEmpty()) {
            return false;
        }
        TypeName parameterType = ctx.parameterType();
        if (IDENTITY_CONTEXT.equals(parameterType)) {
            identityContext(ctx);
            return true;
        }
        if (PRINCIPAL.equals(parameterType)) {
            identityContextValue(ctx, PRINCIPAL, "PIC_PRINCIPAL");
            return true;
        }
        if (AUTHORIZATION_REQUEST.equals(parameterType)) {
            identityContextValue(ctx, AUTHORIZATION_REQUEST, "PIC_AUTHORIZATION_REQUEST");
            return true;
        }
        return false;
    }

    private void identityContext(ParameterCodegenContext ctx) {
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
    }

    private void identityContextValue(ParameterCodegenContext ctx, TypeName valueType, String propertyName) {
        ctx.contentBuilder()
                .addContent(Optional.class)
                .addContentLine(".ofNullable(")
                .increaseContentPadding()
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
                .addContentLine("\"))")
                .addContent(".get(")
                .addContent(AUTH_CONTEXT_REQUEST_FILTER)
                .addContent(".")
                .addContent(propertyName)
                .addContentLine("))")
                .decreaseContentPadding()
                .decreaseContentPadding()
                .addContent(".map(")
                .addContent(valueType)
                .addContentLine(".class::cast)")
                .addContent(".orElseThrow(() -> new ")
                .addContent(NoSuchElementException.class)
                .addContent("(\"")
                .addContent(valueType)
                .addContent(" is not present in request context, required for parameter ")
                .addContent(ctx.paramName())
                .addContentLine("\"));")
                .decreaseContentPadding();
    }
}
