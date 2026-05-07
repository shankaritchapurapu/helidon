/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenUtil;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Constructor;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.ElementSignature;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.codegen.RegistryCodegenContext;
import io.helidon.service.codegen.RegistryRoundContext;
import io.helidon.service.codegen.ServiceCodegenTypes;
import io.helidon.service.codegen.spi.RegistryCodegenExtension;

final class OciMeteringExtension implements RegistryCodegenExtension {
    static final Set<TypeName> ANNOTATIONS = Set.of(OciTypes.METERING_DP_POINT,
                                                    OciTypes.METERING_DP_TIMED,
                                                    OciTypes.METERING_DP_START,
                                                    OciTypes.METERING_DP_END,
                                                    OciTypes.METERING_CP_POINT,
                                                    OciTypes.METERING_CP_TIMED,
                                                    OciTypes.METERING_CP_START,
                                                    OciTypes.METERING_CP_END);
    private static final TypeName GENERATOR = TypeName.create(OciMeteringExtension.class);
    private static final TypeName MAP_STRING_STRING = TypeName.builder()
            .packageName("java.util")
            .className("Map")
            .addTypeArgument(TypeNames.STRING)
            .addTypeArgument(TypeNames.STRING)
            .build();
    private static final TypeName MAP_STRING_INTEGER = TypeName.builder()
            .packageName("java.util")
            .className("Map")
            .addTypeArgument(TypeNames.STRING)
            .addTypeArgument(TypeName.create(Integer.class))
            .build();

    OciMeteringExtension(RegistryCodegenContext ignored) {
    }

    @Override
    public void process(RegistryRoundContext roundContext) {
        Map<TypeName, TypeInfo> knownTypes = roundContext.types()
                .stream()
                .collect(Collectors.toMap(TypeInfo::typeName, Function.identity(), (existing, ignored) -> existing));

        int counter = 0;
        for (TypeName annotation : ANNOTATIONS) {
            for (TypedElementInfo element : roundContext.annotatedElements(annotation)) {
                TypeInfo enclosingType = enclosingType(knownTypes, element);
                generate(roundContext, enclosingType, element, annotation, counter++);
            }
        }
    }

    private TypeInfo enclosingType(Map<TypeName, TypeInfo> knownTypes, TypedElementInfo element) {
        if (element.kind() != ElementKind.METHOD) {
            throw new CodegenException("@Metering annotations are only supported on methods",
                                       element.originatingElementValue());
        }
        TypeName enclosingTypeName = element.enclosingType()
                .orElseThrow(() -> new CodegenException("@Metering method is missing an enclosing type",
                                                        element.originatingElementValue()));
        TypeInfo enclosingType = knownTypes.get(enclosingTypeName);
        if (enclosingType == null) {
            throw new CodegenException("Missing type information for " + enclosingTypeName.fqName(),
                                       element.originatingElementValue());
        }
        return enclosingType;
    }

    private void generate(RegistryRoundContext roundContext,
                          TypeInfo enclosingType,
                          TypedElementInfo element,
                          TypeName annotationType,
                          int counter) {
        MeteringFlavor flavor = MeteringFlavor.of(annotationType);
        MeteringKind kind = MeteringKind.of(annotationType);
        TypeName serviceType = enclosingType.typeName();
        TypeName generatedType = TypeName.builder()
                .packageName(serviceType.packageName())
                .className(serviceType.classNameWithEnclosingNames().replace('.', '_')
                                   + "_" + element.elementName()
                                   + "__Metering" + kind.classNameSuffix()
                                   + (counter == 0 ? "" : "_" + counter))
                .build();

        Annotation annotation = element.annotation(annotationType);
        ParameterIndexes indexes = parameterIndexes(element, flavor);
        Map<String, String> staticTags = staticTags(enclosingType, element, flavor);
        Map<String, Integer> tagIndexes = tagIndexes(element, flavor);
        String methodName = serviceType.fqName() + "." + element.signature().text();

        ClassModel.Builder classModel = ClassModel.builder()
                .type(generatedType)
                .copyright(CodegenUtil.copyright(GENERATOR, serviceType, generatedType))
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR, serviceType, generatedType, "1", ""))
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON))
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_NAMED, methodName))
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .superType(kind.superType(flavor))
                .addImport(Map.class);

        classModel.addField(field -> field
                .accessModifier(AccessModifier.PRIVATE)
                .isFinal(true)
                .type(flavor.supportType())
                .name("meteringSupport"));

        classModel.addConstructor(Constructor.builder()
                                          .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_INJECT))
                                          .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                                          .addParameter(param -> param
                                                  .type(flavor.supportType())
                                                  .name("meteringSupport"))
                                          .addContentLine("this.meteringSupport = meteringSupport;"));

        addSupport(classModel, flavor);
        addStringMethod(classModel, "meterName", annotation.stringValue().orElse(""));
        if (kind.hasCommonMeterFields()) {
            addStringMethod(classModel, "compartmentId", annotation.stringValue("compartmentId").orElse(""));
            addIntMethod(classModel, "compartmentIdParameterIndex", indexes.compartmentId());
            addStringMethod(classModel, "resourceId", annotation.stringValue("resourceId").orElse(""));
            addIntMethod(classModel, "resourceIdParameterIndex", indexes.resourceId());
        }
        if (kind == MeteringKind.POINT) {
            addFloatMethod(classModel, "amount", annotation.floatValue("amount").orElse(1.0F));
            addIntMethod(classModel, "amountParameterIndex", indexes.amount());
        }
        addTags(classModel, "staticTags", staticTags, TypeNames.STRING);
        addTags(classModel, "tagParameterIndexes", tagIndexes, TypeNames.PRIMITIVE_INT);
        addToString(classModel, serviceType, element.signature(), kind);

        roundContext.addGeneratedType(generatedType, classModel, serviceType, element.originatingElementValue());
    }

    private void addSupport(ClassModel.Builder classModel, MeteringFlavor flavor) {
        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(flavor.supportType())
                .name("support")
                .addContentLine("return meteringSupport;"));
    }

    private void addStringMethod(ClassModel.Builder classModel, String name, String value) {
        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(TypeNames.STRING)
                .name(name)
                .addContent("return ")
                .addContentLiteral(value)
                .addContentLine(";"));
    }

    private void addFloatMethod(ClassModel.Builder classModel, String name, float value) {
        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(TypeNames.PRIMITIVE_FLOAT)
                .name(name)
                .addContentLine("return " + value + "F;"));
    }

    private void addIntMethod(ClassModel.Builder classModel, String name, int value) {
        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(TypeNames.PRIMITIVE_INT)
                .name(name)
                .addContentLine("return " + value + ";"));
    }

    private void addTags(ClassModel.Builder classModel,
                         String name,
                         Map<String, ?> values,
                         TypeName valueType) {
        classModel.addMethod(method -> {
            method.addAnnotation(Annotations.OVERRIDE)
                    .accessModifier(AccessModifier.PROTECTED)
                    .returnType(mapType(valueType))
                    .name(name);
            if (values.isEmpty()) {
                method.addContentLine("return Map.of();");
                return;
            }
            method.addContent("return Map.of(");
            boolean first = true;
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                if (!first) {
                    method.addContent(", ");
                }
                first = false;
                method.addContentLiteral(entry.getKey())
                        .addContent(", ");
                Object value = entry.getValue();
                if (value instanceof String stringValue) {
                    method.addContentLiteral(stringValue);
                } else {
                    method.addContent(String.valueOf(value));
                }
            }
            method.addContentLine(");");
        });
    }

    private TypeName mapType(TypeName valueType) {
        if (TypeNames.PRIMITIVE_INT.equals(valueType)) {
            return MAP_STRING_INTEGER;
        }
        return MAP_STRING_STRING;
    }

    private ParameterIndexes parameterIndexes(TypedElementInfo element, MeteringFlavor flavor) {
        int compartmentId = -1;
        int resourceId = -1;
        int amount = -1;
        int index = 0;
        for (TypedElementInfo parameter : element.parameterArguments()) {
            compartmentId = singleIndex(element, parameter, flavor.compartmentIdAnnotation(), compartmentId, index);
            resourceId = singleIndex(element, parameter, flavor.resourceIdAnnotation(), resourceId, index);
            amount = singleIndex(element, parameter, flavor.amountAnnotation(), amount, index);
            index++;
        }
        return new ParameterIndexes(compartmentId, resourceId, amount);
    }

    private int singleIndex(TypedElementInfo element,
                            TypedElementInfo parameter,
                            TypeName annotation,
                            int currentIndex,
                            int newIndex) {
        if (!parameter.hasAnnotation(annotation)) {
            return currentIndex;
        }
        if (currentIndex != -1) {
            throw new CodegenException("Metering annotation supports at most one " + annotation.className()
                                               + " parameter on " + element.signature().text(),
                                       element.originatingElementValue());
        }
        return newIndex;
    }

    private Map<String, Integer> tagIndexes(TypedElementInfo element, MeteringFlavor flavor) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int index = 0;
        for (TypedElementInfo parameter : element.parameterArguments()) {
            int parameterIndex = index;
            parameter.findAnnotation(flavor.tagValueAnnotation())
                    .flatMap(Annotation::stringValue)
                    .ifPresent(tagName -> result.put(tagName, parameterIndex));
            index++;
        }
        return Map.copyOf(result);
    }

    private Map<String, String> staticTags(TypeInfo enclosingType,
                                           TypedElementInfo element,
                                           MeteringFlavor flavor) {
        Map<String, String> result = new LinkedHashMap<>();
        addTags(result, enclosingType.annotations(), flavor);
        addTags(result, element.annotations(), flavor);
        element.findAnnotation(annotationForTags(element, flavor))
                .flatMap(annotation -> annotation.annotationValues("tags"))
                .ifPresent(tags -> addTags(result, tags, flavor));
        return Map.copyOf(result);
    }

    private TypeName annotationForTags(TypedElementInfo element, MeteringFlavor flavor) {
        for (TypeName annotation : ANNOTATIONS) {
            if (flavor.owns(annotation) && element.hasAnnotation(annotation)) {
                return annotation;
            }
        }
        throw new CodegenException("Missing metering annotation for " + element.signature().text(),
                                   element.originatingElementValue());
    }

    private void addTags(Map<String, String> result, List<Annotation> annotations, MeteringFlavor flavor) {
        for (Annotation annotation : annotations) {
            if (flavor.tagAnnotation().equals(annotation.typeName())) {
                putTag(result, annotation);
            } else if (flavor.tagsAnnotation().equals(annotation.typeName())) {
                annotation.annotationValues()
                        .ifPresent(tags -> addTags(result, tags, flavor));
            }
        }
    }

    private void putTag(Map<String, String> result, Annotation annotation) {
        String key = annotation.stringValue("key").orElseThrow();
        String value = annotation.stringValue("value").orElseThrow();
        result.put(key, value);
    }

    private void addToString(ClassModel.Builder classModel,
                             TypeName serviceType,
                             ElementSignature signature,
                             MeteringKind kind) {
        classModel.addMethod(toString -> toString
                .accessModifier(AccessModifier.PUBLIC)
                .returnType(TypeNames.STRING)
                .name("toString")
                .addAnnotation(Annotations.OVERRIDE)
                .addContent("return \"Metering ")
                .addContent(kind.classNameSuffix())
                .addContent(" interceptor for ")
                .addContent(serviceType.fqName())
                .addContent(".")
                .addContent(signature.text())
                .addContentLine("\";"));
    }

    private enum MeteringKind {
        POINT("Point"),
        TIMED("Timed"),
        START("Start"),
        END("End");

        private final String classNameSuffix;

        MeteringKind(String classNameSuffix) {
            this.classNameSuffix = classNameSuffix;
        }

        static MeteringKind of(TypeName annotation) {
            if (OciTypes.METERING_DP_POINT.equals(annotation) || OciTypes.METERING_CP_POINT.equals(annotation)) {
                return POINT;
            }
            if (OciTypes.METERING_DP_TIMED.equals(annotation) || OciTypes.METERING_CP_TIMED.equals(annotation)) {
                return TIMED;
            }
            if (OciTypes.METERING_DP_START.equals(annotation) || OciTypes.METERING_CP_START.equals(annotation)) {
                return START;
            }
            return END;
        }

        String classNameSuffix() {
            return classNameSuffix;
        }

        TypeName superType(MeteringFlavor flavor) {
            return switch (this) {
                case POINT -> flavor.pointMethod();
                case TIMED -> flavor.timedMethod();
                case START -> flavor.startMethod();
                case END -> flavor.endMethod();
            };
        }

        boolean hasCommonMeterFields() {
            return this != END;
        }
    }

    private enum MeteringFlavor {
        DP,
        CP;

        static MeteringFlavor of(TypeName annotation) {
            return annotation.fqName().contains(".metering.dp.") ? DP : CP;
        }

        boolean owns(TypeName annotation) {
            return of(annotation) == this;
        }

        TypeName supportType() {
            return this == DP ? OciTypes.METERING_DP_SUPPORT : OciTypes.METERING_CP_SUPPORT;
        }

        TypeName pointMethod() {
            return this == DP ? OciTypes.METERING_DP_POINT_METHOD : OciTypes.METERING_CP_POINT_METHOD;
        }

        TypeName timedMethod() {
            return this == DP ? OciTypes.METERING_DP_TIMED_METHOD : OciTypes.METERING_CP_TIMED_METHOD;
        }

        TypeName startMethod() {
            return this == DP ? OciTypes.METERING_DP_START_METHOD : OciTypes.METERING_CP_START_METHOD;
        }

        TypeName endMethod() {
            return this == DP ? OciTypes.METERING_DP_END_METHOD : OciTypes.METERING_CP_END_METHOD;
        }

        TypeName tagAnnotation() {
            return this == DP ? OciTypes.METERING_DP_TAG : OciTypes.METERING_CP_TAG;
        }

        TypeName tagsAnnotation() {
            return this == DP ? OciTypes.METERING_DP_TAGS : OciTypes.METERING_CP_TAGS;
        }

        TypeName compartmentIdAnnotation() {
            return this == DP ? OciTypes.METERING_DP_COMPARTMENT_ID : OciTypes.METERING_CP_COMPARTMENT_ID;
        }

        TypeName resourceIdAnnotation() {
            return this == DP ? OciTypes.METERING_DP_RESOURCE_ID : OciTypes.METERING_CP_RESOURCE_ID;
        }

        TypeName amountAnnotation() {
            return this == DP ? OciTypes.METERING_DP_AMOUNT : OciTypes.METERING_CP_AMOUNT;
        }

        TypeName tagValueAnnotation() {
            return this == DP ? OciTypes.METERING_DP_TAG_VALUE : OciTypes.METERING_CP_TAG_VALUE;
        }
    }

    private record ParameterIndexes(int compartmentId, int resourceId, int amount) {
    }
}
