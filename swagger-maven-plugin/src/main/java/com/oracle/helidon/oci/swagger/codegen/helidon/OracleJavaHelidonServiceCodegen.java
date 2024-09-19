/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.swagger.codegen.helidon;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.bmc.sdk.swagger.codegen.OracleCodegenConfig;
import com.oracle.bmc.sdk.swagger.codegen.OracleJavaSdkCodegen;
import com.oracle.bmc.sdk.swagger.codegen.OracleSdkCodegenConfig;
import com.oracle.bmc.sdk.swagger.codegen.docs.DocFormatter;
import com.oracle.bmc.sdk.swagger.codegen.docs.DocFormatter.DescriptionType;
import com.oracle.bmc.sdk.swagger.codegen.docs.JavaDocFormatter;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.CodegenType;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;

/**
 * Generates a Helidon MP service.
 */
public class OracleJavaHelidonServiceCodegen extends OracleJavaSdkCodegen
        implements OracleCodegenConfig {

    /**
     * Reflects the value to be set for this generator.
     */
    public static final String LANGUAGE = "oracle-java-helidon-service";
    private static final DocFormatter DOC_FORMATTER = new JavaDocFormatter();
    private OracleSdkCodegenConfig config;

    /**
     * Default Constructor.
     */
    public OracleJavaHelidonServiceCodegen() {
        super();

        /**
         * This is the classpath location which templates will be read from.
         */
        templateDir = LANGUAGE;
    }

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the CLI generator to select the library
     * with the -l flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return LANGUAGE;
    }

    /**
     * Returns human-friendly help for the generator.
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates service templates for the Oracle Public Infrastructure Cloud that uses Helidon";
    }

    /**
     * Configure this code generator with the provided configuration object.
     *
     * @param config Configuration to apply to this code generator.
     */
    @Override
    public void configure(OracleSdkCodegenConfig config) {
        super.configure(config);
        reset();
        this.config = config;

        setApiPackage(config.getBasePackage() + ".api");

        apiTemplateFiles.clear();
        modelTemplateFiles.clear();

        apiTemplateFiles.put("abstract-resource.mustache", ".java");
    }

    /**
     * List of template files to apply per individual operation defined (vs. per API spec).
     *
     * @return list of per template files.
     */
    @Override
    public List<String> perOperationTemplateFiles() {
        return Collections.emptyList();
    }

    /**
     * List of template files to apply per model defined (vs. per API spec).
     *
     * @return list of per template files.
     */
    @Override
    public List<String> perModelTemplateFilesHttp() {
        return Collections.emptyList();
    }

    /**
     * List of template files to apply per resource defined (vs. per API spec).
     *
     * @return list of per template files.
     */
    @Override
    public List<String> perResourceTemplateFiles() {
        return Collections.emptyList();
    }

    /**
     * Returns Filename of Generated API Class.
     *
     * @param templateName
     * @param tag
     * @return Filename of Generated API Class
     */
    @Override
    public String apiFilename(String templateName, String tag) {
        return apiFileFolder()
                + File.separator
                + "Abstract"
                + toApiFilename(tag)
                + "BaseResource.java";
    }

    /**
     * Converts a Swagger spec operation to the object that will eventually be passed to your template files.
     *
     * @param path        The resource path of the operation.
     * @param httpMethod  The operation's HTTP method.
     * @param operation   The Swagger spec operation.
     * @param definitions A map of {name, code generation model objects} for all model types for this operations
     *                    request and response.
     * @param swagger     The Swagger object with all its swag.
     * @return A code generation operation object that encapsulates all state required to properly generate code for
     * the provided operation.
     */
    @Override
    public CodegenOperation fromOperation(
            String path,
            String httpMethod,
            Operation operation,
            Map<String, Model> definitions,
            Swagger swagger) {
        try {
            // Wrap all the Codegen operations in an Oracle one so we have access to our new properties in our templates
            CodegenOperation fromOperationInDefaultCodegen =
                    super.fromOperationInDefaultCodegen(
                            path, httpMethod, operation, definitions, swagger);

            OracleJavaHelidonServiceCodegenOperation codegenOperation =
                    new OracleJavaHelidonServiceCodegenOperation(
                            this, fromOperationInDefaultCodegen, operation, swagger);

            codegenOperation.notes =
                    DOC_FORMATTER.fixupNotes(
                            codegenOperation.notes,
                            false,
                            DescriptionType.Operation,
                            codegenOperation.isPreviewOnly());

            return codegenOperation;
        } catch (Throwable th) {
            throw new IllegalArgumentException(
                    String.format(
                            "Error parsing spec. Path:%s, Verb:%s, Op:%s",
                            path,
                            httpMethod,
                            operation.getOperationId()),
                    th);
        }
    }

    /**
     * Returns the value of config option set for this service codegen.
     *
     * @param option ConfigOption set for this service codegen
     * @return boolean value of config option
     */
    // only boolean options right now, so implementing in a simple way, no need to support non-booleans yet
    public boolean getOptionValue(ConfigOption option) {
        // values is always set by mojo
        Object v = config.getAdditionalProperties().get(option.additionalPropertyKey);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return Boolean.parseBoolean(
                config.getAdditionalProperties().get(option.additionalPropertyKey));
    }

    /**
     * Template file to apply for Sdk automatic feature metadata (per API spec).
     * <p>
     * This codegen does not use such a template, so returning null.
     *
     * @return always null
     */
    @Override
    public String getSdkClientsMetadataTemplateFile() {
        return null;
    }

    /**
     * Get the file name to apply on Sdk automatic feature metadata template
     * <p>
     * This codegen does not use such a template, so returning null.
     *
     * @param templateName The template that was used
     * @return always null
     */
    @Override
    public String getSdkClientsMetadataFileName(String templateName) {
        return null;
    }

    @Override
    public CodegenParameter fromParameter(Parameter param, Set<String> imports) {
        OracleJavaHelidonServiceCodegenParameter parameter =
                new OracleJavaHelidonServiceCodegenParameter(
                        super.fromParameter(param, imports), this, config.getSpec());

        return parameter;
    }

    // undo things done by the parent codegen
    private void reset() {
        sourceFolder = LANGUAGE + "-sources";
        typeMapping.put("binary", "byte[]");
        typeMapping.put("file", File.class.getSimpleName());
        typeMapping.put("Date", "LocalDate");
    }

    /**
     * Options supported by service for codegen customization.
     */
    public enum ConfigOption {
        /**
         * Option to support validation, enabled by default.
         * Reusing the same option as base so the caller only has to configure one to enable for both models and server stubs.
         */
        OPTION_ENABLE_VALIDATION(OracleJavaSdkCodegen.OPTION_ENABLE_VALIDATION, true),
        /**
         * Option to support identity context, disabled by default.
         */
        OPTION_CONTEXTS_IDENTITY("contextsIdentity", false),
        /**
         * Option to support various Jax-Rs contexts.
         */
        OPTION_CONTEXTS_JAXRS_HTTPHEADERS("contextsJaxRsHttpHeaders", false),
        /**
         * Option to support Jax-Rs Uri Info.
         */
        OPTION_CONTEXTS_JAXRS_URIINFO("contextsJaxRsUriInfo", false),
        /**
         * Option to support Jax-Rs Request.
         */
        OPTION_CONTEXTS_JAXRS_REQUEST("contextsJaxRsRequest", false),
        /**
         * Option to support Jax-Rs secruity context.
         */
        OPTION_CONTEXTS_JAXRS_SECURITYCONTEXT("contextsJaxRsSecurityContext", false),
        /**
         * Option to support auth proxy context.
         */
        OPTION_CONTEXTS_AUTHPROXY_IDENTITY("contextsAuthProxyIdentity", false),
        /**
         * Option to append audit payload.
         */
        OPTION_CONTEXTS_AUDIT_PAYLOAD_APPENDER("contextsAuditPayloadAppender", false),
        /**
         * Option to support container request context.
         */
        OPTION_CONTEXTS_CONTAINER_REQUEST("contextsContainerRequest", false),
        /**
         * Option to support container response context.
         */
        OPTION_CONTEXTS_CONTAINER_RESPONSE("contextsContainerResponse", false),
        /**
         * Option to always return Response objects from APIs.
         */
        OPTION_USE_JAXRS_SERVICE_RESPONSE("useJaxRsServiceResponse", false);

        private final String additionalPropertyKey;
        private final boolean defaultValue;

        ConfigOption(final String additionalPropertyKey, final boolean defaultValue) {
            this.additionalPropertyKey = additionalPropertyKey;
            this.defaultValue = defaultValue;
        }

        /**
         * Returns the key for additionalProperties set as option.
         *
         * @return String
         */
        public String getAdditionalPropertyKey() {
            return this.additionalPropertyKey;
        }

        /**
         * Returns true if the value is default.
         *
         * @return boolean
         */
        public boolean isDefaultValue() {
            return this.defaultValue;
        }
    }
}
