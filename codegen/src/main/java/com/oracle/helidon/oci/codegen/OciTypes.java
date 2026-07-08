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
    static final TypeName WEIGHT = TypeName.create("io.helidon.common.Weight");

    static final TypeName REST_SERVER_ENDPOINT =
            TypeName.create("io.helidon.webserver.http.RestServer.Endpoint");
    static final TypeName HTTP_DELETE = TypeName.create("io.helidon.http.Http.DELETE");
    static final TypeName HTTP_GET = TypeName.create("io.helidon.http.Http.GET");
    static final TypeName HTTP_HEAD = TypeName.create("io.helidon.http.Http.HEAD");
    static final TypeName HTTP_OPTIONS = TypeName.create("io.helidon.http.Http.OPTIONS");
    static final TypeName HTTP_PATCH = TypeName.create("io.helidon.http.Http.PATCH");
    static final TypeName HTTP_POST = TypeName.create("io.helidon.http.Http.POST");
    static final TypeName HTTP_PUT = TypeName.create("io.helidon.http.Http.PUT");
    static final TypeName METRICS_HTTP_ENDPOINT_CONTEXT =
            TypeName.create("com.oracle.helidon.oci.metrics.OciHttpEndpointMetricsContext");
    static final TypeName METRICS_METRIC_PREFIX =
            TypeName.create("com.oracle.helidon.oci.metrics.MetricPrefix");
    static final TypeName METRICS_SECONDARY_METRIC_PREFIX =
            TypeName.create("com.oracle.helidon.oci.metrics.SecondaryMetricPrefix");
    static final TypeName SERVICE_CORE_METRIC_PREFIX =
            TypeName.create("com.oracle.pic.commons.service.metrics.jersey.MetricPrefix");
    static final TypeName SERVICE_CORE_SECONDARY_METRIC_PREFIX =
            TypeName.create("com.oracle.pic.commons.service.metrics.jersey.SecondaryMetricPrefix");

    static final TypeName IDENTITY_AUTHENTICATED =
            TypeName.create("com.oracle.helidon.oci.identity.Identity.Authenticated");

    static final TypeName AUTH_SDK_AUTHORIZATION_PERMISSION =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission");
    static final TypeName AUTH_SDK_AUTHORIZATION_PERMISSIONS =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermissions");
    static final TypeName AUTH_SDK_AUTHORIZE_ASSOCIATE =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizeAssociate");
    static final TypeName AUTH_SDK_AUTHORIZE_CREATE =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizeCreate");
    static final TypeName AUTH_SDK_AUTHORIZE_DELETE =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizeDelete");
    static final TypeName AUTH_SDK_AUTHORIZE_READ_ONLY =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizeReadOnly");
    static final TypeName AUTH_SDK_AUTHORIZE_UPDATE =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.AuthorizeUpdate");
    static final TypeName AUTH_SDK_BODY_VERIFICATION =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.BodyVerification");
    static final TypeName AUTH_SDK_NETWORK_BASED_ACCESS_CONTROL =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.NetworkBasedAccessControl");
    static final TypeName AUTH_SDK_REJECT_CROSS_TENANCY_REQUEST =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.RejectCrossTenancyRequest");
    static final TypeName AUTH_SDK_RESOURCE_ASSOCIATION_REVIEWED =
            TypeName.create(
                    "com.oracle.pic.identity.authorization.permissions.annotations.ResourceAssociationReviewed");
    static final TypeName AUTH_SDK_VARIABLE_OPERATION_NAME =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.VariableOperationName");
    static final TypeName AUTH_SDK_VARIABLE_STRING =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.VariableString");
    static final TypeName AUTH_SDK_VARIABLE_STRINGS =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.VariableStrings");
    static final TypeName AUTH_SDK_ZPR_BASED_ACCESS_CONTROL =
            TypeName.create("com.oracle.pic.identity.authorization.permissions.annotations.ZprBasedAccessControl");

    static final TypeName KIEV_TRANSACTION =
            TypeName.create("com.oracle.helidon.oci.kiev.Kiev.Transaction");
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
