/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.HashSet;
import java.util.Set;

import io.helidon.common.types.TypeName;

/**
 * Annotation groups recognized by the OCI HTTP metrics code generation extension.
 */
final class OciHttpMetricsAnnotations {
    static final Set<TypeName> HTTP_METHODS = Set.of(OciTypes.HTTP_DELETE,
                                                     OciTypes.HTTP_GET,
                                                     OciTypes.HTTP_HEAD,
                                                     OciTypes.HTTP_OPTIONS,
                                                     OciTypes.HTTP_PATCH,
                                                     OciTypes.HTTP_POST,
                                                     OciTypes.HTTP_PUT);

    static final Set<TypeName> METRIC_PREFIX = Set.of(OciTypes.METRICS_METRIC_PREFIX,
                                                      OciTypes.SERVICE_CORE_METRIC_PREFIX);

    static final Set<TypeName> SECONDARY_METRIC_PREFIX = Set.of(OciTypes.METRICS_SECONDARY_METRIC_PREFIX,
                                                                OciTypes.SERVICE_CORE_SECONDARY_METRIC_PREFIX);

    static final Set<TypeName> ALL = all();

    private OciHttpMetricsAnnotations() {
    }

    private static Set<TypeName> all() {
        Set<TypeName> result = new HashSet<>();
        result.add(OciTypes.REST_SERVER_ENDPOINT);
        result.addAll(HTTP_METHODS);
        result.addAll(METRIC_PREFIX);
        result.addAll(SECONDARY_METRIC_PREFIX);
        return Set.copyOf(result);
    }
}
