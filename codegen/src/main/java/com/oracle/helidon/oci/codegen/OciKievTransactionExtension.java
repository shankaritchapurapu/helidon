/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
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

final class OciKievTransactionExtension implements RegistryCodegenExtension {
    private static final String GENERATED_TRANSACTION_NAME_PREFIX = "kt-";
    private static final int KIEV_TRANSACTION_NAME_LIMIT = 80;
    private static final int RUNTIME_TRANSACTION_SUFFIX_MAX_LENGTH = 1 + Long.toString(Long.MIN_VALUE).length();
    static final int TRANSACTION_BASE_NAME_MAX_LENGTH = KIEV_TRANSACTION_NAME_LIMIT
            - RUNTIME_TRANSACTION_SUFFIX_MAX_LENGTH
            - 1;
    private static final int GENERATED_TRANSACTION_NAME_HASH_BYTES = 8;
    private static final TypeName GENERATOR = TypeName.create(OciKievTransactionExtension.class);
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    OciKievTransactionExtension(RegistryCodegenContext ignored) {
    }

    @Override
    public void process(RegistryRoundContext roundContext) {
        Map<TypeName, TypeInfo> knownTypes = roundContext.types()
                .stream()
                .collect(Collectors.toMap(TypeInfo::typeName, Function.identity(), (existing, ignored) -> existing));

        int counter = 0;
        for (TypedElementInfo element : roundContext.annotatedElements(OciTypes.KIEV_TRANSACTION)) {
            TypeInfo enclosingType = enclosingType(knownTypes, element);
            generate(roundContext, enclosingType, element, counter++);
        }
    }

    private TypeInfo enclosingType(Map<TypeName, TypeInfo> knownTypes, TypedElementInfo element) {
        if (element.kind() != ElementKind.METHOD) {
            throw new CodegenException("@KievTransaction is only supported on methods", element.originatingElementValue());
        }
        TypeName enclosingTypeName = element.enclosingType()
                .orElseThrow(() -> new CodegenException("@KievTransaction method is missing an enclosing type",
                                                        element.originatingElementValue()));
        TypeInfo enclosingType = knownTypes.get(enclosingTypeName);
        if (enclosingType == null) {
            throw new CodegenException("Missing type information for " + enclosingTypeName.fqName(),
                                       element.originatingElementValue());
        }
        return enclosingType;
    }

    private void generate(RegistryRoundContext roundContext, TypeInfo enclosingType, TypedElementInfo element, int counter) {
        TypeName serviceType = enclosingType.typeName();
        TypeName generatedType = TypeName.builder()
                .packageName(serviceType.packageName())
                .className(serviceType.classNameWithEnclosingNames().replace('.', '_')
                                   + "_" + element.elementName()
                                   + (counter == 0 ? "" : "_" + counter)
                                   + "__KievTransaction")
                .build();

        String methodName = serviceType.fqName() + "." + element.signature().text();
        Annotation annotation = element.annotation(OciTypes.KIEV_TRANSACTION);
        String transactionName = annotation.stringValue("name")
                .filter(it -> !it.isBlank())
                .map(it -> explicitTransactionName(it, element))
                .orElseGet(() -> defaultTransactionName(methodName,
                                                        element.elementName()));
        String dataStoreName = annotation.stringValue("value")
                .filter(it -> !it.isBlank())
                .orElseThrow(() -> new CodegenException("@KievTransaction value must be set to a configured "
                                                                 + "oci.kiev.data-stores[].store-name on "
                                                                 + element.signature().text(),
                                                         element.originatingElementValue()));
        boolean readOnly = annotation.booleanValue("readOnly").orElse(false);
        int transactionParameterIndex = transactionParameterIndex(element);

        ClassModel.Builder classModel = ClassModel.builder()
                .type(generatedType)
                .copyright(CodegenUtil.copyright(GENERATOR, serviceType, generatedType))
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR, serviceType, generatedType, "1", ""))
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON))
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_NAMED, methodName))
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .superType(OciTypes.KIEV_TRANSACTION_METHOD);

        classModel.addField(field -> field
                .accessModifier(AccessModifier.PRIVATE)
                .isFinal(true)
                .type(OciTypes.KIEV_TRANSACTION_SUPPORT)
                .name("transactionSupport"));

        classModel.addConstructor(Constructor.builder()
                                         .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_INJECT))
                                         .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                                         .addParameter(param -> param
                                                 .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_NAMED,
                                                                                  dataStoreName))
                                                 .type(OciTypes.KIEV_TRANSACTION_SUPPORT)
                                                 .name("transactionSupport"))
                                         .addContentLine("this.transactionSupport = transactionSupport;"));

        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(OciTypes.KIEV_TRANSACTION_SUPPORT)
                .name("support")
                .addContentLine("return transactionSupport;"));

        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(TypeNames.STRING)
                .name("transactionName")
                .addContent("return ")
                .addContentLiteral(transactionName)
                .addContentLine(";"));

        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(TypeNames.PRIMITIVE_BOOLEAN)
                .name("readOnly")
                .addContentLine("return " + readOnly + ";"));

        classModel.addMethod(method -> method
                .addAnnotation(Annotations.OVERRIDE)
                .accessModifier(AccessModifier.PROTECTED)
                .returnType(TypeNames.PRIMITIVE_INT)
                .name("transactionParameterIndex")
                .addContentLine("return " + transactionParameterIndex + ";"));

        addToString(classModel, serviceType, element.signature());

        roundContext.addGeneratedType(generatedType, classModel, serviceType, element.originatingElementValue());
    }

    static String defaultTransactionName(String methodName, String elementName) {
        String hash = hash(methodName);
        String readableName = sanitizeNamePart(elementName);
        int readableNameMaxLength = TRANSACTION_BASE_NAME_MAX_LENGTH
                - GENERATED_TRANSACTION_NAME_PREFIX.length()
                - hash.length()
                - 1;
        if (readableName.length() > readableNameMaxLength) {
            readableName = readableName.substring(0, readableNameMaxLength);
        }
        return GENERATED_TRANSACTION_NAME_PREFIX + readableName + "-" + hash;
    }

    static String explicitTransactionName(String transactionName, String methodSignature) {
        return explicitTransactionName(transactionName, methodSignature, new Object[0]);
    }

    private static String explicitTransactionName(String transactionName, TypedElementInfo element) {
        return explicitTransactionName(transactionName,
                                       element.signature().text(),
                                       element.originatingElementValue());
    }

    private static String explicitTransactionName(String transactionName,
                                                  String methodSignature,
                                                  Object... originatingElements) {
        if (transactionName.length() <= TRANSACTION_BASE_NAME_MAX_LENGTH) {
            return transactionName;
        }
        throw new CodegenException(tooLongExplicitNameMessage(transactionName, methodSignature), originatingElements);
    }

    private static String tooLongExplicitNameMessage(String transactionName, String methodSignature) {
        return """
                @KievTransaction name on %s must be at most %d characters because Helidon appends a runtime suffix \
                and Kiev requires the final transaction name to be below %d characters; got %d characters\
                """.formatted(methodSignature,
                               TRANSACTION_BASE_NAME_MAX_LENGTH,
                               KIEV_TRANSACTION_NAME_LIMIT,
                               transactionName.length());
    }

    private static String sanitizeNamePart(String name) {
        StringBuilder result = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            result.append(Character.isLetterOrDigit(ch) ? ch : '-');
        }
        return result.toString();
    }

    private static String hash(String methodName) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(methodName.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(digest, 0, GENERATED_TRANSACTION_NAME_HASH_BYTES);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }

    private int transactionParameterIndex(TypedElementInfo element) {
        int result = -1;
        int index = 0;
        for (TypedElementInfo parameter : element.parameterArguments()) {
            if (OciTypes.KIEV_CLIENT_TRANSACTION.equals(parameter.typeName())) {
                if (result != -1) {
                    throw new CodegenException("@KievTransaction supports at most one Transaction parameter on "
                                                       + element.signature().text(),
                                               element.originatingElementValue());
                }
                result = index;
            }
            index++;
        }
        return result;
    }

    private void addToString(ClassModel.Builder classModel, TypeName serviceType, ElementSignature signature) {
        classModel.addMethod(toString -> toString
                .accessModifier(AccessModifier.PUBLIC)
                .returnType(TypeNames.STRING)
                .name("toString")
                .addAnnotation(Annotations.OVERRIDE)
                .addContent("return \"Kiev transaction interceptor for ")
                .addContent(serviceType.fqName())
                .addContent(".")
                .addContent(signature.text())
                .addContentLine("\";"));
    }
}
