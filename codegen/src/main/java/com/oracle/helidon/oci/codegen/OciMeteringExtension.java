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
    static final Set<TypeName> ANNOTATIONS = Set.of(OciTypes.METERING_CP_POINT,
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
        ParameterIndexes indexes = parameterIndexes(element);
        Map<String, String> staticTags = staticTags(enclosingType, element);
        Map<String, Integer> tagIndexes = tagIndexes(element);
        String methodName = serviceType.fqName() + "." + element.signature().text();

        ClassModel.Builder classModel = ClassModel.builder()
                .type(generatedType)
                .copyright(CodegenUtil.copyright(GENERATOR, serviceType, generatedType))
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR, serviceType, generatedType, "1", ""))
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON))
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_NAMED, methodName))
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .superType(kind.superType())
                .addImport(Map.class);

        classModel.addField(field -> field
                .accessModifier(AccessModifier.PRIVATE)
                .isFinal(true)
                .type(OciTypes.METERING_CP_SUPPORT)
                .name("meteringSupport"));

        classModel.addConstructor(Constructor.builder()
                                          .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_INJECT))
                                          .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                                          .addParameter(param -> param
                                                  .type(OciTypes.METERING_CP_SUPPORT)
                                                  .name("meteringSupport"))
                                          .addContentLine("this.meteringSupport = meteringSupport;"));

        addSupport(classModel);
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
        addBooleanMethod(classModel, "measureOnFailure", annotation.booleanValue("measureOnFailure").orElse(false));
        addToString(classModel, serviceType, element.signature(), kind);

        roundContext.addGeneratedType(generatedType, classModel, serviceType, element.originatingElementValue());
    }

    private void addSupport(ClassModel.Builder classModel) {
        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(OciTypes.METERING_CP_SUPPORT)
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

    private void addBooleanMethod(ClassModel.Builder classModel, String name, boolean value) {
        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(TypeNames.PRIMITIVE_BOOLEAN)
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

    private ParameterIndexes parameterIndexes(TypedElementInfo element) {
        int compartmentId = -1;
        int resourceId = -1;
        int amount = -1;
        int index = 0;
        for (TypedElementInfo parameter : element.parameterArguments()) {
            compartmentId = singleIndex(element, parameter, OciTypes.METERING_CP_COMPARTMENT_ID, compartmentId, index);
            resourceId = singleIndex(element, parameter, OciTypes.METERING_CP_RESOURCE_ID, resourceId, index);
            amount = singleIndex(element, parameter, OciTypes.METERING_CP_AMOUNT, amount, index);
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

    private Map<String, Integer> tagIndexes(TypedElementInfo element) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int index = 0;
        for (TypedElementInfo parameter : element.parameterArguments()) {
            int parameterIndex = index;
            parameter.findAnnotation(OciTypes.METERING_CP_TAG_VALUE)
                    .flatMap(Annotation::stringValue)
                    .ifPresent(tagName -> result.put(tagName, parameterIndex));
            index++;
        }
        return Map.copyOf(result);
    }

    private Map<String, String> staticTags(TypeInfo enclosingType,
                                           TypedElementInfo element) {
        Map<String, String> result = new LinkedHashMap<>();
        addTags(result, enclosingType.annotations());
        addTags(result, element.annotations());
        element.findAnnotation(annotationForTags(element))
                .flatMap(annotation -> annotation.annotationValues("tags"))
                .ifPresent(tags -> addTags(result, tags));
        return Map.copyOf(result);
    }

    private TypeName annotationForTags(TypedElementInfo element) {
        for (TypeName annotation : ANNOTATIONS) {
            if (element.hasAnnotation(annotation)) {
                return annotation;
            }
        }
        throw new CodegenException("Missing metering annotation for " + element.signature().text(),
                                   element.originatingElementValue());
    }

    private void addTags(Map<String, String> result, List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (OciTypes.METERING_CP_TAG.equals(annotation.typeName())) {
                putTag(result, annotation);
            } else if (OciTypes.METERING_CP_TAGS.equals(annotation.typeName())) {
                annotation.annotationValues()
                        .ifPresent(tags -> addTags(result, tags));
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
            if (OciTypes.METERING_CP_POINT.equals(annotation)) {
                return POINT;
            }
            if (OciTypes.METERING_CP_TIMED.equals(annotation)) {
                return TIMED;
            }
            if (OciTypes.METERING_CP_START.equals(annotation)) {
                return START;
            }
            return END;
        }

        String classNameSuffix() {
            return classNameSuffix;
        }

        TypeName superType() {
            return switch (this) {
                case POINT -> OciTypes.METERING_CP_POINT_METHOD;
                case TIMED -> OciTypes.METERING_CP_TIMED_METHOD;
                case START -> OciTypes.METERING_CP_START_METHOD;
                case END -> OciTypes.METERING_CP_END_METHOD;
            };
        }

        boolean hasCommonMeterFields() {
            return this != END;
        }
    }

    private record ParameterIndexes(int compartmentId, int resourceId, int amount) {
    }
}
