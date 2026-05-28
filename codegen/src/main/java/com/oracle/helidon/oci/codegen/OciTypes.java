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

    static final TypeName METERING_CP_POINT =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.Point");
    static final TypeName METERING_CP_TIMED =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.Timed");
    static final TypeName METERING_CP_START =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.Start");
    static final TypeName METERING_CP_END =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.End");
    static final TypeName METERING_CP_TAG =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.Tag");
    static final TypeName METERING_CP_TAGS =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.Tags");
    static final TypeName METERING_CP_COMPARTMENT_ID =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.CompartmentId");
    static final TypeName METERING_CP_RESOURCE_ID =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.ResourceId");
    static final TypeName METERING_CP_AMOUNT =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.Amount");
    static final TypeName METERING_CP_TAG_VALUE =
            TypeName.create("com.oracle.helidon.oci.metering.cp.Metering.TagValue");
    static final TypeName METERING_CP_POINT_METHOD =
            TypeName.create("com.oracle.helidon.oci.metering.cp.MeteringSupport.PointMethod");
    static final TypeName METERING_CP_TIMED_METHOD =
            TypeName.create("com.oracle.helidon.oci.metering.cp.MeteringSupport.TimedMethod");
    static final TypeName METERING_CP_START_METHOD =
            TypeName.create("com.oracle.helidon.oci.metering.cp.MeteringSupport.StartMethod");
    static final TypeName METERING_CP_END_METHOD =
            TypeName.create("com.oracle.helidon.oci.metering.cp.MeteringSupport.EndMethod");
    static final TypeName METERING_CP_SUPPORT =
            TypeName.create("com.oracle.helidon.oci.metering.cp.MeteringSupport");

    private OciTypes() {
    }
}
