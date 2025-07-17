/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.maven.swagger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.bmc.sdk.swagger.codegen.OracleCodegenOrchestrator;
import com.oracle.bmc.sdk.swagger.codegen.OracleCodegenOrchestratorInput;
import com.oracle.bmc.sdk.swagger.codegen.OracleJavaSdkCodegen;
import com.oracle.bmc.sdk.swagger.codegen.SpecGenerationType;
import com.oracle.helidon.oci.swagger.codegen.helidon.OracleJavaHelidonServiceCodegen;
import io.swagger.codegen.utils.OptionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Generates code using Swagger.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class OracleSwaggerMojo extends AbstractMojo {
    private static final String DUMMY_TEST_DIR =
            "dummy_tests"; // not used, but needed by SDK codegen
    private static final String DEFAULT_LOGGER_NAME =
            "log"; // default name used for logging, which is not used by sdk codegen
    private static final String DEFAULT_TRUE = Boolean.TRUE.toString();
    private static final String DEFAULT_FALSE = Boolean.FALSE.toString();

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * The Swagger specification path. This may be relative or an absolute path.
     */
    @Parameter(defaultValue = "src/main/resources/swagger.yaml")
    private String specPath;

    /**
     * Location to put the output.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources")
    private String outputDir;

    /**
     * The package name to prefix all generated classes with, ex, "com.oracle.oci.myservice".
     */
    @Parameter(required = true)
    private String basePackage;

    /**
     * A map of classes and the import that should be used for that class.
     */
    @Parameter(name = "importMappings")
    private List<String> importMappings;

    @Parameter(defaultValue = OracleJavaHelidonServiceCodegen.LANGUAGE)
    private String language;

    /**
     * Additional properties to pass forward to the SDK code generator.
     */
    @Parameter
    private Map<String, Object> additionalProperties;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (OracleSwaggerMojo.class) {
            if (additionalProperties == null) {
                additionalProperties = Collections.emptyMap();
            }
            if (importMappings == null) {
                importMappings = Collections.emptyList();
            }

            setAdditionalProperties();
            normalizeAdditionalProperties();
            generateModels();
            generateApis();

            // Register the compiled sources as a source root
            // null check for unit test
            if (project != null) {
                project.addCompileSourceRoot(outputDir);
            }
        }
    }

    // adjust the types from String -> boolean (as needed)
    protected void normalizeAdditionalProperties() {
        Set<String> keys = new LinkedHashSet<>(additionalProperties.keySet());
        for (String key : keys) {
            if (key.startsWith("use") || key.startsWith("enable") || key.startsWith("contexts")) {
                normalizeAdditionalProperty(key);
            }
        }
    }

    protected void normalizeAdditionalProperty(String key) {
        Object v = additionalProperties.get(key);
        if (v instanceof String) {
            additionalProperties.put(key, Boolean.parseBoolean((String) v));
        }
    }

    /**
     * In Swagger, you can specify <importMappings/> with multiple <importMapping/> child nodes.
     * Generally, each importMapping should be a single map from Swagger class to external model class,
     * but you can specify multiple maps separated by commas. Example:
     *
     * <importMappings>
     *     <importMapping>
     *         ClassA=com.oracle.pic.ClassA
     *     </importMapping>
     *     <importMapping>
     *         ClassB=com.oracle.pic.ClassB,ClassC=com.oracle.pic.ClassC,ClassD=com.oracle.pic.ClassD
     *     </importMapping>
     * </importMappings>
     *
     * @return Map Swagger class to external model class
     */
    public Map<String, String> createMapFromImportMappings() {
        Map<String, String> ret = new HashMap<>();
        for (String importMapping : importMappings) {
            for (Map.Entry<String, String> entry
                    : createMapFromKeyValuePairs(importMapping).entrySet()) {
                ret.put(entry.getKey().trim(), entry.getValue().trim());
            }
        }
        return ret;
    }

    // even if client generation is disabled, a bunch of stuff gets generated that we don't want.
    private static void deleteExtras(String basePath) throws MojoExecutionException {
        String internalFolder = basePath + File.separatorChar + "internal";
        String requestsFolder = basePath + File.separatorChar + "requests";
        String responsesFolder = basePath + File.separatorChar + "responses";
        try {
            FileUtils.deleteDirectory(new File(internalFolder));
            FileUtils.deleteDirectory(new File(requestsFolder));
            FileUtils.deleteDirectory(new File(responsesFolder));
            new File(basePath, "SdkClientsMetadata.java").delete();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to remove extra classes", e);
        }
    }

    private static Map<String, String> createMapFromKeyValuePairs(String commaSeparatedKVPairs) {
        final List<Pair<String, String>> pairs =
                OptionUtils.parseCommaSeparatedTuples(commaSeparatedKVPairs);

        Map<String, String> result = new HashMap<>();

        for (Pair<String, String> pair : pairs) {
            result.put(pair.getLeft(), pair.getRight());
        }

        return result;
    }

    private void setAdditionalProperties() {
        if (!additionalProperties.containsKey(OracleJavaSdkCodegen.OPTION_ANNOTATION_PACKAGE)) {
            additionalProperties.put(OracleJavaSdkCodegen.OPTION_ANNOTATION_PACKAGE, "jakarta");
        }
    }

    private void generateModels() throws MojoExecutionException, MojoFailureException {
        Map<String, Object> modelAdditionalProperties = new HashMap<>(additionalProperties);

        // user can always override these, but if they don't, default these values
        modelAdditionalProperties.putIfAbsent(
                OracleJavaSdkCodegen.OPTION_USE_EXPLICITLY_SET_FILTER, DEFAULT_FALSE);

        // for new apis starting off with polymorphic types, force type to be required (no default
        // will be added, spec should make sure no default is specified).
        // for apis migrating to polymorphic types, the 'default' type will be picked up from spec
        // and added.
        modelAdditionalProperties.putIfAbsent(
                OracleJavaSdkCodegen.OPTION_ENABLE_DEFAULT_DISCRIMINATOR_ONLY_ELSE_NONE,
                DEFAULT_TRUE);

        // turn on generation of 'fromString' factory methods for enums, needed for dropwizard
        // to deserialize enums with underscores correctly
        modelAdditionalProperties.putIfAbsent(
                OracleJavaSdkCodegen.OPTION_ENABLE_ENUM_FROM_STRING, DEFAULT_TRUE);

        // default validation matches what is configured for the service
        modelAdditionalProperties.putIfAbsent(
                OracleJavaSdkCodegen.OPTION_ENABLE_VALIDATION,
                String.valueOf(
                        OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_ENABLE_VALIDATION.isDefaultValue()));

        // assuming most teams will not override the default name, so configure SDK to use the default logger name
        modelAdditionalProperties.putIfAbsent(
                OracleJavaSdkCodegen.OPTION_LOGGER_NAME, DEFAULT_LOGGER_NAME);

        // unless told otherwise, use case-insensitive enums
        modelAdditionalProperties.putIfAbsent(
                OracleJavaSdkCodegen.USE_CASE_INSENSITIVE_ENUMS_NAME, "true");

        runCodegen(OracleJavaSdkCodegen.LANGUAGE, modelAdditionalProperties);

        String generatedPath =
                outputDir
                        + File.separatorChar
                        + OracleJavaSdkCodegen.LANGUAGE
                        + "-sources"
                        + File.separatorChar
                        + basePackage.replace('.', File.separatorChar);
        deleteExtras(generatedPath);
    }

    private void generateApis() throws MojoExecutionException, MojoFailureException {
        Map<String, Object> serviceAdditionalProperties = new HashMap<>(additionalProperties);

        // add default values for everything that wasn't specified by the caller
        for (OracleJavaHelidonServiceCodegen.ConfigOption configOption : OracleJavaHelidonServiceCodegen.ConfigOption.values()) {
            serviceAdditionalProperties.putIfAbsent(
                    configOption.getAdditionalPropertyKey(),
                    configOption.isDefaultValue());
        }

        runCodegen(language, serviceAdditionalProperties);
    }

    private void runCodegen(String language, Map<String, Object> additionalProperties)
            throws MojoExecutionException, MojoFailureException {
        try {
            OracleCodegenOrchestratorInput codegenOrchestratorInput =
                    OracleCodegenOrchestratorInput.builder()
                            .specPath(specPath)
                            .outputDir(outputDir)
                            .basePackage(basePackage)
                            .language(language)
                            .specGenerationType(SpecGenerationType.INTERNAL.name())
                            .generateClient(false)
                            .additionalProperties((Map) additionalProperties)
                            .isTestGenerationEnabled(false)
                            .testOutputDir(DUMMY_TEST_DIR)
                            .externalModels(createMapFromImportMappings())
                            .isGenerateUnknownForResponseEnumsEnabled(false)
                            .build();

            // to setup anything we don't explicitly configure
            codegenOrchestratorInput.setDefaults();

            new OracleCodegenOrchestrator().orchestrate(codegenOrchestratorInput);
        } catch (RuntimeException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (AssertionError e) {
            throw new MojoFailureException("Failed to parse Swagger spec from: " + specPath, e);
        }
    }

    /**
     * Sets Swagger specification path. This may be relative or an absolute path.
     *
     * @param specPath String
     */
    public void setSpecPath(String specPath) {
        this.specPath = specPath;
    }

    /**
     * Sets location to put the output.
     *
     * @param outputDir String
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Sets package name to prefix all generated classes with, ex, "com.oracle.oci.myservice".
     *
     * @param basePackage String
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * A map of classes and the import that should be used for that class.
     *
     * @param importMappings List<String>
     */
    public void setImportMappings(List<String> importMappings) {
        this.importMappings = importMappings;
    }

    /**
     * Specify what type of code generator to use, defaults to oracle-java-helidon-service.
     *
     * @param language String
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Additional properties to pass forward to the SDK code generator.
     *
     * @param additionalProperties Map<String, Object>
     */
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
