/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import io.helidon.common.types.TypeName;

final class OciTypes {

    static final TypeName HTTP_ENTRYPOINT_INTERCEPTOR = TypeName.create("io.helidon.webserver.http.HttpEntryPoint.Interceptor");
    static final TypeName INTERCEPTOR_CONTEXT = TypeName.create("io.helidon.service.registry.InterceptionContext");
    static final TypeName INTERCEPTOR_CHAIN = TypeName.create("io.helidon.webserver.http.HttpEntryPoint.Interceptor.Chain");
    static final TypeName SERVER_REQUEST = TypeName.create("io.helidon.webserver.http.ServerRequest");
    static final TypeName SERVER_RESPONSE = TypeName.create("io.helidon.webserver.http.ServerResponse");

    static final TypeName AUTHORIZATION_PERMISSION =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission");
    static final TypeName KIEV_TRANSACTION =
            TypeName.create("com.oracle.helidon.oci.kiev.KievTransaction");
    static final TypeName KIEV_TRANSACTION_METHOD =
            TypeName.create("com.oracle.helidon.oci.kiev.KievTransactionSupport.TransactionMethod");
    static final TypeName KIEV_TRANSACTION_SUPPORT =
            TypeName.create("com.oracle.helidon.oci.kiev.KievTransactionSupport");
    static final TypeName KIEV_CLIENT_TRANSACTION =
            TypeName.create("com.oracle.pic.kiev.Transaction");

    private OciTypes() {
    }
}
