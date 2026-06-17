/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.List;
import java.util.Map;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OciHttpMetricsExtensionTest {
    private static final TypeName ENDPOINT = TypeName.create("example.StoreEndpoint");

    @Test
    void defaultScopeUsesSimpleClassAndMethodName() {
        OciHttpMetricsExtension.EndpointScopes scopes =
                OciHttpMetricsExtension.scopes(ENDPOINT, List.of(), "list", List.of());

        assertEquals("StoreEndpoint.list", scopes.primaryScope());
        assertEquals(List.of(), scopes.secondaryScopes());
    }

    @Test
    void metricPrefixOverridesDefaultScope() {
        OciHttpMetricsExtension.EndpointScopes scopes =
                OciHttpMetricsExtension.scopes(ENDPOINT,
                                               List.of(metricPrefix("StoreApi", false)),
                                               "list",
                                               List.of());

        assertEquals("StoreApi", scopes.primaryScope());
    }

    @Test
    void metricPrefixCanAppendMethodName() {
        OciHttpMetricsExtension.EndpointScopes scopes =
                OciHttpMetricsExtension.scopes(ENDPOINT,
                                               List.of(metricPrefix("StoreApi", true)),
                                               "list",
                                               List.of());

        assertEquals("StoreApi.list", scopes.primaryScope());
    }

    @Test
    void secondaryMetricPrefixesComeFromClassAndMethod() {
        OciHttpMetricsExtension.EndpointScopes scopes =
                OciHttpMetricsExtension.scopes(ENDPOINT,
                                               List.of(secondaryMetricPrefix("StoreClass")),
                                               "list",
                                               List.of(secondaryMetricPrefix("StoreList")));

        assertEquals("StoreEndpoint.list", scopes.primaryScope());
        assertEquals(List.of("StoreClass", "StoreList"), scopes.secondaryScopes());
    }

    @Test
    void endpointMethodIncludesFullyQualifiedResourceClassName() {
        OciHttpMetricsExtension.EndpointMethod endpointMethod =
                OciHttpMetricsExtension.endpointMethod(ENDPOINT, List.of(), "list", "list()", List.of());

        assertEquals("example.StoreEndpoint::list()", endpointMethod.methodKey());
        assertEquals("example.StoreEndpoint", endpointMethod.fullyQualifiedResourceClassName());
    }

    @Test
    void generatedTypeUsesResourceClassName() {
        TypeName generatedType = OciHttpMetricsExtension.generatedType(ENDPOINT);

        assertEquals("example.StoreEndpoint__HttpMetricsInterceptor", generatedType.fqName());
    }

    @Test
    void serviceCoreAnnotationsAreRecognized() {
        OciHttpMetricsExtension.EndpointScopes scopes =
                OciHttpMetricsExtension.scopes(ENDPOINT,
                                               List.of(Annotation.create(OciTypes.SERVICE_CORE_METRIC_PREFIX,
                                                                         Map.of("value", "Compat",
                                                                                "appendMethodName", true)),
                                                       Annotation.create(OciTypes.SERVICE_CORE_SECONDARY_METRIC_PREFIX,
                                                                         "CompatSecondary")),
                                               "get",
                                               List.of());

        assertEquals("Compat.get", scopes.primaryScope());
        assertEquals(List.of("CompatSecondary"), scopes.secondaryScopes());
    }

    private static Annotation metricPrefix(String value, boolean appendMethodName) {
        return Annotation.create(OciTypes.METRICS_METRIC_PREFIX,
                                 Map.of("value", value, "appendMethodName", appendMethodName));
    }

    private static Annotation secondaryMetricPrefix(String value) {
        return Annotation.create(OciTypes.METRICS_SECONDARY_METRIC_PREFIX, value);
    }
}
