package com.oracle.helidon.oci.swagger.codegen.helidon;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.oracle.bmc.sdk.swagger.codegen.model.java.OracleJavaCodegenOperation;
import com.oracle.bmc.sdk.swagger.codegen.model.java.OracleJavaCodegenParameter;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenParameter;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import static java.lang.String.format;

/**
 * Represents an operation and its metadata required to generate a Java JaxRS Client that relies on
 * PIC Commons' RestClient.
 */
@Slf4j
public class OracleJavaHelidonServiceCodegenOperation extends OracleJavaCodegenOperation {
    private static final String VENDOR_EXTENSION_CAPTURE_PATTERN = "x-capture-pattern";
    /**
     * Paginated APIs declare at least one of these headers to indicate the resulting list of items
     * is paginated.
     */
    //    private static final Set<String> PAGINATION_RESPONSE_HEADER_NAMES =
    //            Sets.newHashSet("opc-limit", "opc-next-page", "opc-previous-page", "opc-total-count");

    /**
     *  APIs declared to have etag in the response header, need to return TaggedResponse
     */
    //    private static final Set<String> TAGGED_RESPONSE_HEADER_NAMES = Sets.newHashSet("etag");

    /**
     * Most of the time, the result type of the abstract resource operation is the same as the
     * result type of the client methods.
     *
     * However, in some situations, the result type of the operation is different. For example, for
     * paginated responses, the client result is a Page and the abstract resource is a
     * PaginatedResponse.
     *
     * In case of HTTP HEAD operation, no entity is returned. Just the headers are returned
     */
    public String abstractResourceReturnType;

    public List<String> contextsToInclude = new ArrayList<>();

    private CodegenOperation codegenOperation;

    /**
     * Copy the field values from the provided operation to a new Oracle-specific operation and
     * generate additional fields for use in templates.
     *
     * @param original The operation to copy.
     */
    public OracleJavaHelidonServiceCodegenOperation(
            @NonNull OracleJavaHelidonServiceCodegen oracleJavaHelidonServiceCodegen,
            @NonNull CodegenOperation original,
            Operation baseOperation,
            @NonNull Swagger spec) {
        super(oracleJavaHelidonServiceCodegen, original, baseOperation, spec);
        this.codegenOperation = original;
        log.info("Starting operation " + operationId);

        // Update params to be fully qualified
        fixFullyQualifiedModelClassNames(oracleJavaHelidonServiceCodegen.modelPackage());

        // Update return type and set return type for the abstract method
        resolveReturnType(
                oracleJavaHelidonServiceCodegen.modelPackage(), oracleJavaHelidonServiceCodegen);

        // add configured contexts
        resolveContexts(oracleJavaHelidonServiceCodegen);

        resolvePathCapturePattern(original, baseOperation);

        log.info("Finished operation " + operationId);
    }

    // mustache is too complicated to make this calculate in the template, just expose it as a method
    public boolean hasParamsAndContexts() {
        return allParams != null && !allParams.isEmpty() && !contextsToInclude.isEmpty();
    }

    private void fixFullyQualifiedModelClassNames(String modelPackage) {

        if (this.allParams != null) {
            for (CodegenParameter param : this.allParams) {
                // case is important for types
                if ("File".equals(param.dataType)) {
                    param.dataType = "java.io.File";
                    param.baseType = param.dataType;
                }

                // for everything else, only process header and query params, skip body param
                if (BooleanUtils.isNotTrue(param.isBodyParam)) {
                    fixEnumTypes(param, modelPackage);
                } else {
                    // in case this is a body param, we also need to fix up the fully qualified name
                    fixFullyQualifiedModelClassNamesForBodyParam(modelPackage, param);
                }
            }
        }

        // repeat for query/header params
        if (this.queryParams != null) {
            for (CodegenParameter param : this.queryParams) {
                if (BooleanUtils.isNotTrue(param.isQueryParam)) {
                    continue;
                }
                fixEnumTypes(param, modelPackage);
            }
        }

        if (this.headerParams != null) {
            for (CodegenParameter param : this.headerParams) {
                if (BooleanUtils.isNotTrue(param.isHeaderParam)) {
                    continue;
                }
                fixEnumTypes(param, modelPackage);
            }
        }

        // update body params
        fixFullyQualifiedModelClassNamesForBodyParam(modelPackage, this.bodyParam);
    }

    private void fixFullyQualifiedModelClassNamesForBodyParam(
            String modelPackage, CodegenParameter bodyParam) {
        if (bodyParam == null || BooleanUtils.isNotTrue(bodyParam.isBodyParam)) {
            return;
        }
        log.info("Body Param " + bodyParam.dataType.toString());
        boolean isBodyParam = BooleanUtils.isTrue(bodyParam.isBodyParam);
        boolean isNonPrimitiveType = BooleanUtils.isNotTrue(bodyParam.isPrimitiveType);

        /*
         * If we have a non-primitive body param, let's fully qualify the data-type
         *
         * Entering this block:
         * dataType = List<Wibble>
         * baseType = Wibble
         *
         * Exiting this block:
         * dataType = List<com.oracle.pic.fqdn.Wibble>
         * baseType = com.oracle.pic.fqdn.Wibble
         */
        if (isByte(bodyParam.dataType)) {
            if (isLargePayload()) {
                bodyParam.dataType = "java.io.InputStream";
            } else {
                bodyParam.dataType = "byte[]";
            }
            bodyParam.baseType = bodyParam.dataType;
        } else if (isBodyParam && isNonPrimitiveType) {
            String fullyQualifiedBaseType = format("%s.%s", modelPackage, bodyParam.baseType);
            if (!bodyParam.dataType.contains(modelPackage)) {
                bodyParam.dataType =
                        bodyParam.dataType.replace(bodyParam.baseType, fullyQualifiedBaseType);
            }
            if (!bodyParam.baseType.contains(modelPackage)) {
                bodyParam.baseType = fullyQualifiedBaseType;
            }
        }
    }

    private void fixEnumTypes(CodegenParameter param, String modelPackage) {
        if (param instanceof OracleJavaCodegenParameter) {
            OracleJavaCodegenParameter ojcp = (OracleJavaCodegenParameter) param;
            if (ojcp.getTopLevelEnum() != null) {
                param.dataType = ojcp.getTopLevelEnum();
            } else if (ojcp.getEnumReferenceClass() != null) {
                String fullyQualifiedBaseType = format("%s.%s", modelPackage, param.dataType);
                if (!param.dataType.contains(modelPackage)) {
                    param.dataType = fullyQualifiedBaseType;
                }
            }
        }
    }

    private void resolveContexts(OracleJavaHelidonServiceCodegen oracleJavaHelidonServiceCodegen) {
        if (oracleJavaHelidonServiceCodegen.getOptionValue(OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_IDENTITY)) {
            contextsToInclude.add(
                    "@com.oracle.pic.identity.authorization.sdk.context.PrincipalContext com.oracle.pic.identity.authentication.Principal principal");
            contextsToInclude.add(
                    "@com.oracle.pic.identity.authorization.sdk.context.AuthorizationRequestContext com.oracle.pic.identity.authorization.sdk.AuthorizationRequest authorizationRequest");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_JAXRS_HTTPHEADERS)) {
            contextsToInclude.add(
                    "@"
                            + annotationPackage
                            + ".ws.rs.core.Context "
                            + annotationPackage
                            + ".ws.rs.core.HttpHeaders httpHeadersContext");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_JAXRS_URIINFO)) {
            contextsToInclude.add(
                    "@"
                            + annotationPackage
                            + ".ws.rs.core.Context "
                            + annotationPackage
                            + ".ws.rs.core.UriInfo uriInfoContext");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_JAXRS_REQUEST)) {
            contextsToInclude.add(
                    "@"
                            + annotationPackage
                            + ".ws.rs.core.Context "
                            + annotationPackage
                            + ".ws.rs.core.Request requestContext");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_JAXRS_SECURITYCONTEXT)) {
            contextsToInclude.add(
                    "@"
                            + annotationPackage
                            + ".ws.rs.core.Context "
                            + annotationPackage
                            + ".ws.rs.core.SecurityContext securityContext");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_AUTHPROXY_IDENTITY)) {
            contextsToInclude.add(
                    "@"
                            + annotationPackage
                            + ".ws.rs.core.Context com.oracle.pic.authproxy.AuthProxyIdentity authProxyIdentity");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_AUDIT_PAYLOAD_APPENDER)) {
            contextsToInclude.add(
                    "@com.oracle.pic.sherlock.collector.jersey.AuditContext com.oracle.pic.sherlock.collector.AuditPayloadAppender auditPayloadAppender");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_CONTAINER_REQUEST)) {
            contextsToInclude.add(
                    "@"
                            + annotationPackage
                            + ".ws.rs.core.Context "
                            + annotationPackage
                            + ".ws.rs.container.ContainerRequestContext containerRequestContext");
        }
        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_CONTEXTS_CONTAINER_RESPONSE)) {
            contextsToInclude.add(
                    "@"
                            + annotationPackage
                            + ".ws.rs.core.Context "
                            + annotationPackage
                            + ".ws.rs.container.ContainerResponseContext containerResponseContext");
        }
    }

    private void resolveReturnType(
            String modelPackage, OracleJavaHelidonServiceCodegen oracleJavaHelidonServiceCodegen) {

        httpMethod = annotationPackage + ".ws.rs." + httpMethod;

        if (oracleJavaHelidonServiceCodegen.getOptionValue(
                OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_USE_JAXRS_SERVICE_RESPONSE)) {
            returnType = annotationPackage + ".ws.rs.core.Response";
            abstractResourceReturnType = returnType;
            return;
        }

        if ("Void".equals(returnType)) {
            returnType = null; // undo what the parent gen does
        }

        // HTTP HEAD is a special case operation where only the headers are returned
        // Actual Entity is not returned
        if ((annotationPackage + ".ws.rs.HEAD").equals(httpMethod)) {
            returnType = annotationPackage + ".ws.rs.core.MultivaluedMap<String, String>";
        }

        // Most of the time, the abstract and client return types are the same
        abstractResourceReturnType = returnType;

        // TODO: Add Pagination and Tagging Support
        //        if (oracleJavaHelidonServiceCodegen.getOptionValue(
        //                ConfigOption.OPTION_USE_TAGGED_MODELS_WITH_ETAGS)) {
        //            if (responseHeaders
        //                            .stream()
        //                            .anyMatch(
        //                                    (param) ->
        //                                            PAGINATION_RESPONSE_HEADER_NAMES.contains(
        //                                                    param.baseName))
        //                    && responseHeaders
        //                            .stream()
        //                            .anyMatch(
        //                                    (param) ->
        //                                            TAGGED_RESPONSE_HEADER_NAMES.contains(
        //                                                    param.baseName))) {
        //                throw new RuntimeException(
        //                        "Conflicting response header found : both of "
        //                                + PAGINATION_RESPONSE_HEADER_NAMES
        //                                + " and "
        //                                + TAGGED_RESPONSE_HEADER_NAMES);
        //            }
        //
        //            if (responseHeaders
        //                    .stream()
        //                    .anyMatch((param) -> TAGGED_RESPONSE_HEADER_NAMES.contains(param.baseName))) {
        //                if ("array".equals(returnContainer)) {
        //                    throw new RuntimeException("Can't have Collection(array) of TaggedResponse");
        //                }
        //                // in case there is no return type, returnType and returnBaseType will both be null
        //                if (returnBaseType == null) {
        //                    returnBaseType = "Void";
        //                }
        //                returnType =
        //                        "com.oracle.pic.commons.service.model.TaggedResponse<"
        //                                + returnBaseType
        //                                + ">";
        //                abstractResourceReturnType =
        //                        "com.oracle.pic.commons.service.model.TaggedResponse<"
        //                                + returnBaseType
        //                                + ">";
        //            }
        //        }
        //
        //        if (responseHeaders
        //                .stream()
        //                .anyMatch((param) -> PAGINATION_RESPONSE_HEADER_NAMES.contains(param.baseName))) {
        //            if ("array".equals(returnContainer)) {
        //                abstractResourceReturnType =
        //                        "com.oracle.pic.commons.service.model.PaginatedResponse<"
        //                                + returnBaseType
        //                                + ">";
        //            } else if (returnBaseType.endsWith("Collection")
        //                    || returnBaseType.endsWith("Aggregation")) {
        //                // this only works with server stubs
        //                abstractResourceReturnType =
        //                        "com.oracle.pic.commons.service.model.PaginatedCollectionResponse<"
        //                                + returnBaseType
        //                                + ">";
        //            }
        //        }

        /*
         * For our generated model data-types, we'd like to fully qualify references to those
         * in the return types of our operations.
         *
         * Some examples:
         *
         * 1) For operations that return an 'array' of Wibbles (not primitives like String)
         *
         * Entering this block:
         *
         * returnBaseType = "Wibble"
         * returnType = "Page<Wibble>"
         * abstractResourceReturnType = "PaginatedResponse<Wibble>"
         *
         * Leaving this block:
         *
         * returnBaseType = "com.oracle.pic.fqdn.Wibble"
         * returnType = "Page<com.oracle.pic.fqdn.Wibble>"
         * abstractResourceReturnType = "PaginatedResponse<com.oracle.pic.fqdn.Wibble>"
         * abstractResourceReturnType = "PaginatedResponse<com.oracle.pic.fqdn.Wibble>"
         *
         * 2) For non-'array' & also not-primitive return types
         *
         * Entering this block:
         *
         * returnBaseType = "Wibble"
         * returnType = "Wibble"
         * abstractResourceReturnType = "Wibble"
         *
         * Leaving this block:
         *
         * returnBaseType = "com.oracle.pic.fqdn.Wibble"
         * returnType = "com.oracle.pic.fqdn.Wibble"
         * abstractResourceReturnType = "com.oracle.pic.fqdn.Wibble"
         *
         */
        if (isByte(returnBaseType)) {
            if (isLargePayload()) {
                returnBaseType = "java.io.InputStream";
            } else {
                returnBaseType = "byte[]";
            }
            returnType = returnBaseType;
            abstractResourceReturnType = returnBaseType;
        }
        if ("File".equals(returnBaseType)) {
            returnBaseType = "java.io.File";
            returnType = returnBaseType;
            abstractResourceReturnType = returnBaseType;
        }
        if ("Map".equals(returnBaseType)) {
            returnBaseType = "java.util.Map";
            returnType = returnBaseType;
            abstractResourceReturnType = returnBaseType;
        }
    }

    private void resolvePathCapturePattern(
            CodegenOperation originalOperation, Operation baseOperation) {
        for (Parameter param : baseOperation.getParameters()) {
            if (isPathParamWithCapture(param)) {
                final String capturePattern =
                        param.getVendorExtensions()
                                .get(VENDOR_EXTENSION_CAPTURE_PATTERN)
                                .toString();
                this.validateCapturePattern(capturePattern);
                final String paramSegment = String.format("{%s}", param.getName());
                final String newParamSegment =
                        String.format("{%s: %s}", param.getName(), capturePattern);
                // update path only if this param is being used in path
                // usually spec validations will not allow that to happen but this
                // is to cover the case if the validation was disabled
                if (originalOperation.path.contains(paramSegment)) {
                    this.path = originalOperation.path.replace(paramSegment, newParamSegment);
                }
            }
        }
    }

    private void validateCapturePattern(final String capturePattern) {
        if (StringUtils.isBlank(capturePattern)) {
            throw new RuntimeException(
                    String.format(
                            "Regex pattern for %s cannot be blank",
                            VENDOR_EXTENSION_CAPTURE_PATTERN));
        }
        try {
            Pattern.compile(capturePattern);
        } catch (PatternSyntaxException ex) {
            throw new RuntimeException(
                    String.format(
                            "Invalid %s regex pattern: %s",
                            VENDOR_EXTENSION_CAPTURE_PATTERN,
                            capturePattern));
        }
    }

    private boolean isPathParamWithCapture(Parameter param) {
        return param.getIn().equalsIgnoreCase("path")
                && param.getVendorExtensions().containsKey(VENDOR_EXTENSION_CAPTURE_PATTERN);
    }

    private boolean isLargePayload() {
        // for large responses, byte[] is not good, allow an InputStream to be used so we don't have to buffer.
        // this is UNTESTED in dropwizard, it may require you to manually set the content length header, or it may
        // buffer anyway.
        if (codegenOperation.vendorExtensions == null) {
            return false;
        }
        Object extension = codegenOperation.vendorExtensions.get("x-obmcs-large-response-payload");
        if (Boolean.TRUE.equals(extension)) {
            return true;
        }

        return false;
    }

    private static boolean isByte(String type) {
        return "Byte[]".equals(type) || "byte[]".equals(type);
    }
}
