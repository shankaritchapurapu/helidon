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
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.codegen.RegistryCodegenContext;
import io.helidon.service.codegen.RegistryRoundContext;
import io.helidon.service.codegen.ServiceCodegenTypes;
import io.helidon.service.codegen.spi.RegistryCodegenExtension;

class OciAuthorizationExtension implements RegistryCodegenExtension {

    private static final String PACKAGE_NAME_PREFIX = "com.oracle.pic.identity.authorization.permissions.annotations";

    private final RegistryCodegenContext ctx;
    private final Map<String, Set<String>> methods = new HashMap<>();

    OciAuthorizationExtension(RegistryCodegenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void process(RegistryRoundContext roundContext) {
        // init processing
        methods.clear();

        // collect all methods and group them by package
        for (TypeInfo typeInfo : roundContext.types()) {
            for (TypedElementInfo elementInfo : typeInfo.elementInfo()) {
                List<Annotation> annotations = elementInfo.allAnnotations();
                if (!annotations.isEmpty() && needsInterception(annotations)) {
                    String packageName = typeInfo.typeName().packageName();
                    Set<String> set = methods.computeIfAbsent(packageName, k -> new HashSet<>());
                    set.add(typeInfo.typeName().toString() + "::" + elementInfo.signature().toString());
                }
            }
        }

        // generate an interceptor per package
        for (Map.Entry<String, Set<String>> entry : methods.entrySet()) {
            Set<String> methodElements = entry.getValue();
            TypeName generatedType = TypeName.builder()
                    .packageName(entry.getKey())
                    .className("Authorization_interceptor")
                    .build();
            generateInterceptor(roundContext, generatedType, methodElements);
        }
    }

    private boolean needsInterception(List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.typeName().toString().startsWith(PACKAGE_NAME_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private void generateInterceptor(RegistryRoundContext roundContext,
                                     TypeName generatedType,
                                     Set<String> methodElements) {
        ClassModel.Builder builder = ClassModel.builder()
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addAnnotation(Annotation.create(ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON))
                .type(generatedType)
                .addInterface(OciTypes.HTTP_ENTRYPOINT_INTERCEPTOR)
                .sortStaticFields(false);

        builder.addImport(TypeNames.SET)
                .addImport(TypeNames.TYPE_NAME)
                .addImport(TypeNames.TYPED_ELEMENT_INFO)
                .addImport(TypeNames.OPTIONAL)
                .addImport(URI.class)
                .addImport(HashSet.class)
                .addImport("javax.ws.rs.WebApplicationException")
                .addImport("javax.ws.rs.core.Response")
                .addImport("io.helidon.service.registry.Services")
                .addImport("com.oracle.pic.identity.authorization.sdk.IAuthorizationClient")
                .addImport("com.oracle.pic.identity.authentication.AuthenticatorClient")
                .addImport("com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter")
                .addImport("com.oracle.helidon.oci.jaxrs.HelidonContainerRequestContext")
                .addImport("com.oracle.helidon.oci.jaxrs.HelidonContextInjector")
                .addImport("com.oracle.helidon.oci.jaxrs.HelidonResourceInfo")
                .addImport("com.oracle.helidon.oci.identity.AuthorizationClientFactory");

        builder.addField(field -> field.name("LOGGER")
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type("System.Logger")
                .addContent("System.getLogger(\"" + generatedType.name() + "\")"));

        Field.Builder fieldBuilder = Field.builder();
        fieldBuilder.name("INTERCEPTED_METHODS")
                .isStatic(true)
                .isFinal(true)
                .accessModifier(AccessModifier.PRIVATE)
                .type("Set<String>")
                .addContent("new HashSet(Set.of(\n")
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

        if (INTERCEPTED_METHODS.contains(method)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Intercepting call '" + typedElementInfo.signature() + "'");

            // get clients from registry
            AuthenticatorClient authnClient = Services.get(AuthenticatorClient.class);
            Optional<IAuthorizationClient> authzClient = Services.first(IAuthorizationClient.class);

            // create an initialize filter
            AuthContextRequestFilter filter = authzClient.isEmpty() ? new AuthContextRequestFilter(authnClient)
                    : new AuthContextRequestFilter(authnClient, authzClient.get());
            HelidonResourceInfo resourceInfo = new HelidonResourceInfo(serviceType, methodSignature);
            HelidonContainerRequestContext context = new HelidonContainerRequestContext(request, resourceInfo);
            HelidonContextInjector.inject(filter, context);
            HelidonContextInjector.postConstruct(filter);

            // call filter after buffering entity
            request.content().buffer();
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
        }

        chain.proceed(request, response);"""));

        roundContext.addGeneratedType(generatedType,
                                      builder,
                                      generatedType);
    }
}
