/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Field;
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

class OciAuthorizationExtension implements RegistryCodegenExtension {
    private final RegistryCodegenContext ctx;
    private final Map<String, Set<String>> authenticatedMethods = new HashMap<>();
    private final Map<String, Set<String>> authorizedMethods = new HashMap<>();

    OciAuthorizationExtension(RegistryCodegenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void process(RegistryRoundContext roundContext) {
        // init processing
        authenticatedMethods.clear();
        authorizedMethods.clear();

        // collect all methods and group them by package
        for (TypeInfo typeInfo : roundContext.types()) {
            boolean classLevelAuthenticated = hasAnnotation(typeInfo.allAnnotations(), OciTypes.IDENTITY_AUTHENTICATED);
            for (TypedElementInfo elementInfo : typeInfo.elementInfo()) {
                if (elementInfo.kind() != ElementKind.METHOD) {
                    continue;
                }
                List<Annotation> annotations = elementInfo.allAnnotations();
                MethodIdentityMode mode = identityMode(annotations, classLevelAuthenticated);
                if (mode != null) {
                    String packageName = typeInfo.typeName().packageName();
                    Map<String, Set<String>> methods = mode == MethodIdentityMode.AUTHORIZED
                            ? authorizedMethods
                            : authenticatedMethods;
                    Set<String> set = methods.computeIfAbsent(packageName, k -> new HashSet<>());
                    set.add(typeInfo.typeName().toString() + "::" + elementInfo.signature().toString());
                }
            }
        }

        // generate an interceptor per package
        Set<String> packages = new HashSet<>();
        packages.addAll(authenticatedMethods.keySet());
        packages.addAll(authorizedMethods.keySet());
        for (String packageName : packages) {
            Set<String> authenticatedMethodElements = authenticatedMethods.getOrDefault(packageName, Set.of());
            Set<String> authorizedMethodElements = authorizedMethods.getOrDefault(packageName, Set.of());
            TypeName generatedType = TypeName.builder()
                    .packageName(packageName)
                    .className("Authorization_interceptor")
                    .build();
            generateInterceptor(roundContext, generatedType, authenticatedMethodElements, authorizedMethodElements);
        }
    }

    private MethodIdentityMode identityMode(List<Annotation> annotations, boolean classLevelAuthenticated) {
        MethodIdentityMode result = classLevelAuthenticated ? MethodIdentityMode.AUTHENTICATED : null;
        for (Annotation annotation : annotations) {
            TypeName typeName = annotation.typeName();
            if (OciAuthorizationAnnotations.AUTHORIZED.contains(typeName)) {
                return MethodIdentityMode.AUTHORIZED;
            }
            if (OciAuthorizationAnnotations.AUTHENTICATED.contains(typeName)) {
                result = MethodIdentityMode.AUTHENTICATED;
            }
        }
        return result;
    }

    private boolean hasAnnotation(List<Annotation> annotations, TypeName typeName) {
        for (Annotation annotation : annotations) {
            if (typeName.equals(annotation.typeName())) {
                return true;
            }
        }
        return false;
    }

    private void generateInterceptor(RegistryRoundContext roundContext,
                                     TypeName generatedType,
                                     Set<String> authenticatedMethodElements,
                                     Set<String> authorizedMethodElements) {
        ClassModel.Builder builder = ClassModel.builder()
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON))
                .type(generatedType)
                .addInterface(OciTypes.HTTP_ENTRYPOINT_INTERCEPTOR)
                .sortStaticFields(false);

        builder.addImport(TypeNames.SET)
                .addImport(TypeNames.MAP)
                .addImport(TypeNames.TYPE_NAME)
                .addImport(TypeNames.TYPED_ELEMENT_INFO)
                .addImport(URI.class)
                .addImport(HashSet.class)
                .addImport("javax.ws.rs.WebApplicationException")
                .addImport("javax.ws.rs.core.Response")
                .addImport("io.helidon.service.registry.Services")
                .addImport("com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter")
                .addImport("com.oracle.helidon.oci.identity.AuthContextRequestFilterFactory")
                .addImport("com.oracle.helidon.oci.jaxrs.HelidonContainerRequestContext")
                .addImport("com.oracle.helidon.oci.jaxrs.HelidonContextInjector")
                .addImport("com.oracle.helidon.oci.jaxrs.HelidonResourceInfo")
                .addImport("com.oracle.helidon.oci.identity.IdentityContext");

        builder.addField(field -> field.name("LOGGER")
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type("System.Logger")
                .addContent("System.getLogger(\"" + generatedType.name() + "\")"));

        addMethodSetField(builder, "AUTHENTICATED_METHODS", authenticatedMethodElements);
        addMethodSetField(builder, "AUTHORIZED_METHODS", authorizedMethodElements);

        builder.addMethod(proceed -> proceed.addAnnotation(Annotations.OVERRIDE)
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
                .addContentLine("""
        TypedElementInfo typedElementInfo = interceptionContext.elementInfo();
        String serviceType = interceptionContext.serviceInfo().serviceType().toString();
        String methodSignature = typedElementInfo.signature().toString();
        String method = serviceType + "::" + methodSignature;
        boolean authorized = AUTHORIZED_METHODS.contains(method);

        if (authorized || AUTHENTICATED_METHODS.contains(method)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Intercepting call '" + typedElementInfo.signature() + "'");

            // create and initialize filter
            AuthContextRequestFilterFactory filterFactory = Services.get(AuthContextRequestFilterFactory.class);
            AuthContextRequestFilter filter = authorized ? filterFactory.create() : filterFactory.createAuthenticatedOnly();
            HelidonResourceInfo resourceInfo = new HelidonResourceInfo(serviceType, methodSignature);
            HelidonContainerRequestContext context = new HelidonContainerRequestContext(request, resourceInfo);
            HelidonContextInjector.inject(filter, context);
            HelidonContextInjector.postConstruct(filter);

            // Buffering a bodyless request delegates to an empty entity implementation,
            // whose default buffer() method throws UnsupportedOperationException.
            if (context.hasEntity()) {
                request.content().buffer();
            }
            try {
                filter.filter(context);
            } catch (WebApplicationException e) {
                Response res = e.getResponse();
                if (res.hasEntity()) {
                    response.status(res.getStatus()).send(res.getEntity());
                } else {
                    response.status(res.getStatus()).send();
                }
                return;
            }

            if (context.isAborted()) {
                String msg = context.getAbortMessage();
                response.status(context.getAbortStatus()).send(msg != null ? msg : "");
                return;
            }

            // register authentication context
            Map<String, Object> properties = context.properties();
            request.context().register(new IdentityContext(properties));
        }

        chain.proceed(request, response);"""));

        roundContext.addGeneratedType(generatedType,
                                      builder,
                                      generatedType);
    }

    private void addMethodSetField(ClassModel.Builder builder, String name, Set<String> methodElements) {
        Field.Builder fieldBuilder = Field.builder();
        fieldBuilder.name(name)
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type("Set<String>");
        if (methodElements.isEmpty()) {
            fieldBuilder.addContent("new HashSet<>()");
            builder.addField(fieldBuilder.build());
            return;
        }

        fieldBuilder.addContent("new HashSet<>(Set.of(\n")
                .increaseContentPadding();
        boolean first = true;
        for (String methodElement : methodElements) {
            if (!first) {
                fieldBuilder.addContent(",\n");
            }
            first = false;
            fieldBuilder.addContent("\"" + methodElement + "\"");
        }
        fieldBuilder.addContent("))");
        builder.addField(fieldBuilder.build());
    }

    private enum MethodIdentityMode {
        AUTHENTICATED,
        AUTHORIZED
    }
}
