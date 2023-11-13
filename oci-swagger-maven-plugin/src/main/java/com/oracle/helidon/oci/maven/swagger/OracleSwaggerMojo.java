package com.oracle.helidon.oci.maven.swagger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.helidon.oci.swagger.codegen.helidon.OracleJavaHelidonServiceCodegen;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.oracle.bmc.sdk.swagger.codegen.OracleCodegenOrchestrator;
import com.oracle.bmc.sdk.swagger.codegen.OracleCodegenOrchestratorInput;
import com.oracle.bmc.sdk.swagger.codegen.OracleJavaSdkCodegen;
import com.oracle.bmc.sdk.swagger.codegen.SpecGenerationType;

import io.swagger.codegen.utils.OptionUtils;
import lombok.Setter;

/**
 * Generates code using Swagger.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class OracleSwaggerMojo extends AbstractMojo {
    private static final String DUMMY_TEST_DIR =
            "dummy_tests"; // not used, but needed by SDK codegen
    private static final String DEFAULT_LOMBOK_SLF4J_LOGGER_NAME =
            "log"; // default name used by lombok, which is not used by sdk codegen
    private static final String DEFAULT_TRUE = Boolean.TRUE.toString();
    private static final String DEFAULT_FALSE = Boolean.FALSE.toString();

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * The Swagger specification path. This may be relative or an absolute path.
     */
    @Parameter(defaultValue = "src/main/resources/swagger.yaml")
    @Setter
    private String specPath;

    /**
     * Location to put the output.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources")
    @Setter
    private String outputDir;

    /**
     * The package name to prefix all generated classes with, ex, "com.oracle.oci.myservice"
     */
    @Parameter(required = true)
    @Setter
    private String basePackage;

    /**
     * A map of classes and the import that should be used for that class
     */
    @Parameter(name = "importMappings")
    @Setter
    private List<String> importMappings;

    @Parameter(defaultValue = OracleJavaHelidonServiceCodegen.LANGUAGE)
    @Setter
    private String language;

    /**
     * Additional properties to pass forward to the SDK code generator.
     */
    @Parameter @Setter private Map<String, String> additionalProperties;

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
            generateModels();
            generateApis();

            // Register the compiled sources as a source root
            // null check for unit test
            if (project != null) {
                project.addCompileSourceRoot(outputDir);
            }
        }
    }

    private void setAdditionalProperties() {
        if (!additionalProperties.containsKey(OracleJavaSdkCodegen.OPTION_ANNOTATION_PACKAGE)) {
            String v =
                    additionalProperties.get(
                            OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_USE_JAKARTA_ANNOTATIONS.getAdditionalPropertyKey());
            if (v != null) {
                boolean useJakartaAnnotations = Boolean.valueOf(v);
                if (useJakartaAnnotations) {
                    additionalProperties.put(
                            OracleJavaSdkCodegen.OPTION_ANNOTATION_PACKAGE, "jakarta");
                }
            }
        } else {
            if (additionalProperties.containsKey(OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_USE_JAKARTA_ANNOTATIONS.name())) {
                throw new IllegalArgumentException(
                        "Cannot combine "
                                + OracleJavaHelidonServiceCodegen.ConfigOption.OPTION_USE_JAKARTA_ANNOTATIONS.name()
                                + " and "
                                + OracleJavaSdkCodegen.OPTION_ANNOTATION_PACKAGE);
            }
        }
    }

    private void generateModels() throws MojoExecutionException, MojoFailureException {
        Map<String, String> modelAdditionalProperties = new HashMap<>(additionalProperties);

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

        // assuming most teams will not override the lombok default name, so configure SDK to use the default logger name
        modelAdditionalProperties.putIfAbsent(
                OracleJavaSdkCodegen.OPTION_LOGGER_NAME, DEFAULT_LOMBOK_SLF4J_LOGGER_NAME);

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

    private void generateApis() throws MojoExecutionException, MojoFailureException {
        Map<String, String> serviceAdditionalProperties = new HashMap<>(additionalProperties);

        // add default values for everything that wasn't specified by the caller
        for (OracleJavaHelidonServiceCodegen.ConfigOption configOption : OracleJavaHelidonServiceCodegen.ConfigOption.values()) {
            serviceAdditionalProperties.putIfAbsent(
                    configOption.getAdditionalPropertyKey(),
                    Boolean.valueOf(configOption.isDefaultValue()).toString());
        }

        runCodegen(language, serviceAdditionalProperties);
    }

    private void runCodegen(String language, Map<String, String> additionalProperties)
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
                            .additionalProperties(additionalProperties)
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
     * In Swagger, you can specify <importMappings> with multiple <importMapping> child nodes.
     * Generally, each importMapping should be a single map from Swagger class to external model class,
     * but you can specify multiple maps separated by commas. Example:
     * <importMappings>
     *     <importMapping>
     *         ClassA=com.oracle.pic.ClassA
     *     </importMapping>
     *     <importMapping>
     *         ClassB=com.oracle.pic.ClassB,ClassC=com.oracle.pic.ClassC,ClassD=com.oracle.pic.ClassD
     *     </importMapping>
     * </importMappings>
     */
    public Map<String, String> createMapFromImportMappings() {
        Map<String, String> ret = new HashMap<>();
        for (String importMapping : importMappings) {
            for (Map.Entry<String, String> entry :
                    createMapFromKeyValuePairs(importMapping).entrySet()) {
                ret.put(entry.getKey().trim(), entry.getValue().trim());
            }
        }
        return ret;
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
}
