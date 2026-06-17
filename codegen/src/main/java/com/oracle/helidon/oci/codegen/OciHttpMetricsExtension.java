/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.codegen.RegistryCodegenContext;
import io.helidon.service.codegen.RegistryRoundContext;
import io.helidon.service.codegen.ServiceCodegenTypes;
import io.helidon.service.codegen.spi.RegistryCodegenExtension;

final class OciHttpMetricsExtension implements RegistryCodegenExtension {
    private static final double INTERCEPTOR_WEIGHT = 1050.0;

    private final Map<TypeName, List<EndpointMethod>> endpointMethods = new HashMap<>();

    OciHttpMetricsExtension(RegistryCodegenContext ignored) {
    }

    @Override
    public void process(RegistryRoundContext roundContext) {
        endpointMethods.clear();
        if (roundContext.typeInfo(OciTypes.METRICS_HTTP_ENDPOINT_CONTEXT).isEmpty()) {
            return;
        }

        for (TypeInfo typeInfo : roundContext.types()) {
            if (!hasAnnotation(typeInfo.allAnnotations(), OciTypes.REST_SERVER_ENDPOINT)) {
                continue;
            }
            for (TypedElementInfo elementInfo : typeInfo.elementInfo()) {
                if (elementInfo.kind() == ElementKind.METHOD && isHttpMethod(elementInfo)) {
                    endpointMethods.computeIfAbsent(typeInfo.typeName(), ignored -> new ArrayList<>())
                            .add(endpointMethod(typeInfo, elementInfo));
                }
            }
        }

        for (Map.Entry<TypeName, List<EndpointMethod>> entry : endpointMethods.entrySet()) {
            generateInterceptor(roundContext, generatedType(entry.getKey()), entry.getValue());
        }
    }

    private EndpointMethod endpointMethod(TypeInfo typeInfo, TypedElementInfo elementInfo) {
        return endpointMethod(typeInfo.typeName(),
                              typeInfo.allAnnotations(),
                              elementInfo.elementName(),
                              elementInfo.signature().toString(),
                              elementInfo.allAnnotations());
    }

    static EndpointMethod endpointMethod(TypeName typeName,
                                         List<Annotation> typeAnnotations,
                                         String methodName,
                                         String methodSignature,
                                         List<Annotation> methodAnnotations) {
        EndpointScopes scopes = scopes(typeName, typeAnnotations, methodName, methodAnnotations);
        return new EndpointMethod(typeName + "::" + methodSignature,
                                  scopes.primaryScope(),
                                  scopes.secondaryScopes(),
                                  typeName.toString());
    }

    static TypeName generatedType(TypeName resourceType) {
        return TypeName.builder()
                .packageName(resourceType.packageName())
                .className(resourceType.classNameWithEnclosingNames().replace('.', '_')
                                   + "__HttpMetricsInterceptor")
                .build();
    }

    static EndpointScopes scopes(TypeInfo typeInfo, TypedElementInfo elementInfo) {
        return scopes(typeInfo.typeName(),
                      typeInfo.allAnnotations(),
                      elementInfo.elementName(),
                      elementInfo.allAnnotations());
    }

    static EndpointScopes scopes(TypeName typeName,
                                 List<Annotation> typeAnnotations,
                                 String methodName,
                                 List<Annotation> methodAnnotations) {
        String defaultScope = typeName.className() + "." + methodName;
        String primaryScope = metricPrefix(typeAnnotations, methodName).orElse(defaultScope);

        List<String> secondaryScopes = new ArrayList<>();
        metricValue(typeAnnotations, OciHttpMetricsAnnotations.SECONDARY_METRIC_PREFIX)
                .ifPresent(secondaryScopes::add);
        metricValue(methodAnnotations, OciHttpMetricsAnnotations.SECONDARY_METRIC_PREFIX)
                .ifPresent(secondaryScopes::add);

        return new EndpointScopes(primaryScope, List.copyOf(secondaryScopes));
    }

    private static java.util.Optional<String> metricPrefix(List<Annotation> annotations, String methodName) {
        for (Annotation annotation : annotations) {
            if (OciHttpMetricsAnnotations.METRIC_PREFIX.contains(annotation.typeName())) {
                String value = annotation.stringValue().orElse("");
                boolean appendMethodName = annotation.booleanValue("appendMethodName").orElse(false);
                return java.util.Optional.of(appendMethodName ? value + "." + methodName : value);
            }
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<String> metricValue(List<Annotation> annotations, Set<TypeName> annotationTypes) {
        for (Annotation annotation : annotations) {
            if (annotationTypes.contains(annotation.typeName())) {
                return annotation.stringValue();
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean isHttpMethod(TypedElementInfo elementInfo) {
        return hasAnnotation(elementInfo.allAnnotations(), OciHttpMetricsAnnotations.HTTP_METHODS);
    }

    private static boolean hasAnnotation(List<Annotation> annotations, TypeName typeName) {
        for (Annotation annotation : annotations) {
            if (typeName.equals(annotation.typeName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotation(List<Annotation> annotations, Set<TypeName> typeNames) {
        for (Annotation annotation : annotations) {
            if (typeNames.contains(annotation.typeName())) {
                return true;
            }
        }
        return false;
    }

    private void generateInterceptor(RegistryRoundContext roundContext,
                                     TypeName generatedType,
                                     List<EndpointMethod> methods) {
        ClassModel.Builder builder = ClassModel.builder()
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON))
                .addAnnotation(Annotation.create(OciTypes.WEIGHT, Map.of("value", INTERCEPTOR_WEIGHT)))
                .type(generatedType)
                .addInterface(OciTypes.HTTP_ENTRYPOINT_INTERCEPTOR)
                .addImport(TypeNames.LIST)
                .addImport(TypeNames.TYPED_ELEMENT_INFO)
                .addImport("com.oracle.helidon.oci.metrics.OciHttpEndpointMetricsContext");

        builder.addMethod(proceed -> {
            proceed.addAnnotation(Annotations.OVERRIDE)
                    .returnType(TypeNames.PRIMITIVE_VOID)
                    .accessModifier(AccessModifier.PUBLIC)
                    .name("proceed")
                    .addParameter(p -> p.type(OciTypes.INTERCEPTOR_CONTEXT)
                            .name("interceptionContext"))
                    .addParameter(p -> p.type(OciTypes.INTERCEPTOR_CHAIN)
                            .name("chain"))
                    .addParameter(p -> p.type(OciTypes.SERVER_REQUEST)
                            .name("request"))
                    .addParameter(p -> p.type(OciTypes.SERVER_RESPONSE)
                            .name("response"))
                    .addThrows(t -> t.type(Exception.class))
                    .addContentLine("TypedElementInfo typedElementInfo = interceptionContext.elementInfo();")
                    .addContentLine("String serviceType = interceptionContext.serviceInfo().serviceType().toString();")
                    .addContentLine("String methodSignature = typedElementInfo.signature().toString();")
                    .addContentLine("String method = serviceType + \"::\" + methodSignature;")
                    .addContentLine("String primaryScope = null;")
                    .addContentLine("List<String> secondaryScopes = List.of();")
                    .addContentLine("String fullyQualifiedResourceClassName = null;");

            boolean first = true;
            for (EndpointMethod endpointMethod : methods) {
                proceed.addContent(first ? "if (" : " else if (")
                        .addContentLiteral(endpointMethod.methodKey())
                        .addContentLine(".equals(method)) {")
                        .increaseContentPadding()
                        .addContent("primaryScope = ")
                        .addContentLiteral(endpointMethod.primaryScope())
                        .addContentLine(";")
                        .addContent("secondaryScopes = ");
                addStringList(proceed, endpointMethod.secondaryScopes());
                proceed.addContentLine(";")
                        .addContent("fullyQualifiedResourceClassName = ")
                        .addContentLiteral(endpointMethod.fullyQualifiedResourceClassName())
                        .addContentLine(";")
                        .decreaseContentPadding()
                        .addContentLine("}");
                first = false;
            }

            proceed.addContentLine("""

        if (primaryScope == null) {
            chain.proceed(request, response);
            return;
        }

        if (fullyQualifiedResourceClassName == null) {
            // Guard against future generated metadata drift between matched endpoint fields.
            chain.proceed(request, response);
            return;
        }

        OciHttpEndpointMetricsContext metricsContext =
                new OciHttpEndpointMetricsContext(primaryScope, secondaryScopes, fullyQualifiedResourceClassName);
        request.context().register(metricsContext);
        boolean failed = false;
        metricsContext.markResourceStart();
        try {
            chain.proceed(request, response);
        } catch (Exception e) {
            failed = true;
            throw e;
        } finally {
            metricsContext.markResourceEnd(failed);
        }""");
        });

        roundContext.addGeneratedType(generatedType, builder, generatedType);
    }

    private void addStringList(io.helidon.codegen.classmodel.Method.Builder method, List<String> values) {
        if (values.isEmpty()) {
            method.addContent("List.of()");
            return;
        }
        method.addContent("List.of(");
        boolean first = true;
        for (String value : values) {
            if (!first) {
                method.addContent(", ");
            }
            first = false;
            method.addContentLiteral(value);
        }
        method.addContent(")");
    }

    record EndpointScopes(String primaryScope, List<String> secondaryScopes) {
    }

    record EndpointMethod(String methodKey,
                          String primaryScope,
                          List<String> secondaryScopes,
                          String fullyQualifiedResourceClassName) {
    }
}
