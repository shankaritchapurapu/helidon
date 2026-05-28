/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.HashSet;
import java.util.Set;

import io.helidon.common.types.TypeName;

/**
 * Annotation groups recognized by the OCI authorization code generation
 * extension.
 */
final class OciAuthorizationAnnotations {
    /**
     * Annotations that require authentication and request identity propagation,
     * but do not require an authorization permission check.
     */
    static final Set<TypeName> AUTHENTICATED = Set.of(OciTypes.IDENTITY_AUTHENTICATED,
                                                      OciTypes.AUTH_SDK_BODY_VERIFICATION);

    /**
     * Annotations that require both authentication and authorization handling.
     */
    static final Set<TypeName> AUTHORIZED = Set.of(OciTypes.AUTH_SDK_AUTHORIZATION_PERMISSION,
                                                   OciTypes.AUTH_SDK_AUTHORIZATION_PERMISSIONS,
                                                   OciTypes.AUTH_SDK_AUTHORIZE_ASSOCIATE,
                                                   OciTypes.AUTH_SDK_AUTHORIZE_CREATE,
                                                   OciTypes.AUTH_SDK_AUTHORIZE_DELETE,
                                                   OciTypes.AUTH_SDK_AUTHORIZE_READ_ONLY,
                                                   OciTypes.AUTH_SDK_AUTHORIZE_UPDATE,
                                                   OciTypes.AUTH_SDK_NETWORK_BASED_ACCESS_CONTROL,
                                                   OciTypes.AUTH_SDK_REJECT_CROSS_TENANCY_REQUEST,
                                                   OciTypes.AUTH_SDK_RESOURCE_ASSOCIATION_REVIEWED,
                                                   OciTypes.AUTH_SDK_VARIABLE_OPERATION_NAME,
                                                   OciTypes.AUTH_SDK_VARIABLE_STRING,
                                                   OciTypes.AUTH_SDK_VARIABLE_STRINGS,
                                                   OciTypes.AUTH_SDK_ZPR_BASED_ACCESS_CONTROL);

    /**
     * Full annotation set advertised to Helidon codegen discovery.
     */
    static final Set<TypeName> ALL = all();

    private OciAuthorizationAnnotations() {
    }

    private static Set<TypeName> all() {
        Set<TypeName> result = new HashSet<>();
        result.addAll(AUTHENTICATED);
        result.addAll(AUTHORIZED);
        return Set.copyOf(result);
    }
}
