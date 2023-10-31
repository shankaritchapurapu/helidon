package io.helidon.integrations.oci.maven.swagger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.oracle.bmc.sdk.swagger.codegen.preprocess.PreProcessor.PreProcessorError;
import com.oracle.bmc.sdk.swagger.codegen.preprocess.PreProcessorException;
import io.helidon.integrations.oci.swagger.codegen.helidon.OracleJavaHelidonServiceCodegen;
import io.helidon.integrations.oci.swagger.codegen.helidon.OracleJavaHelidonServiceCodegen.ConfigOption;
import io.swagger.codegen.Codegen;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;

public class OracleJavaHelidonServiceCodegenTest {

    @TempDir public Path temp;

    @Test
    public void verifyRegisteredAsCodegenConfigServiceProvider() {
        assertThat(Codegen.getConfig(OracleJavaHelidonServiceCodegen.LANGUAGE), notNullValue());
    }

    @Test
    public void abstractBaseFile_generateWithDefaultValues() throws Exception {
        final File abstractResourceFile =
                abstractTestGeneration("AbstractComputeBaseResource.java", true, false, true, null);
        assertThat("true", abstractResourceFile.exists());
        List<String> linesFromFile = FileUtils.readLines(abstractResourceFile, "UTF-8");

        assertThat(find(linesFromFile, "public abstract byte[] getBinaryString"), is(true));
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.NotNull"), is(true));
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.Pattern"), is(true));
        assertThat(
                find(
                        linesFromFile,
                        "@com.oracle.pic.identity.authorization.sdk.context.PrincipalContext com.oracle.pic.identity"
                                + ".authentication.Principal principal"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context jakarta.ws.rs.core.HttpHeaders httpHeadersContext"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context com.oracle.pic.authproxy.AuthProxyIdentity authProxyIdentity"), is(false));

        File abstractPrimitiveBodyBaseResourceFile =
                new File(
                        abstractResourceFile.getParentFile(),
                        "AbstractPrimitiveBodyBaseResource.java");
        assertThat(abstractPrimitiveBodyBaseResourceFile.exists(), is(true));
        linesFromFile = FileUtils.readLines(abstractPrimitiveBodyBaseResourceFile, "UTF-8");

        assertThat(find(linesFromFile, ".String stringBody"), is(false));
    }

    @Test
    public void abstractBaseFile_generateWithAllOptionsSetToNonDefaultValues() throws Exception {
        final File abstractResourceFile =
                abstractTestGeneration(
                        "AbstractComputeBaseResource.java", false, false, true, null);
        assertThat(abstractResourceFile.exists(), is(true));
        List<String> linesFromFile = FileUtils.readLines(abstractResourceFile, "UTF-8");

        assertThat(find(linesFromFile, "public abstract byte[] getBinaryString"), is(true));
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.NotNull"), is(false));
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.Pattern"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@com.oracle.pic.identity.authorization.sdk.context.PrincipalContext com.oracle.pic.identity"
                                + ".authentication.Principal principal"), is(true));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context jakarta.ws.rs.core.HttpHeaders httpHeadersContext"), is(true));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context com.oracle.pic.authproxy.AuthProxyIdentity authProxyIdentity"), is(true));
    }

    @Test
    public void abstractBaseFile_generateWithAdditionalProperties() throws Exception {
        Map<String, String> customAdditionalProperties =
                ImmutableMap.of("contextsAuditPayloadAppender", "true");
        final File abstractResourceFile =
                abstractTestGeneration(
                        "AbstractComputeBaseResource.java",
                        true,
                        false,
                        true,
                        customAdditionalProperties);
        assertThat(abstractResourceFile.exists(), is(true));
        List<String> linesFromFile = FileUtils.readLines(abstractResourceFile, "UTF-8");

        // Check explicitly for expected entries based on the provided additionalProperties
        assertThat(
                find(
                        linesFromFile,
                        "@com.oracle.pic.sherlock.collector.jersey.AuditContext com.oracle.pic.sherlock.collector"
                                + ".AuditPayloadAppender"), is(true));

        // Check for common values in disabled state.
        assertThat(find(linesFromFile, "public abstract byte[] getBinaryString"), is(true));
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.NotNull"), is(true));
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.Pattern"), is(true));
        assertThat(
                find(
                        linesFromFile,
                        "@com.oracle.pic.identity.authorization.sdk.context.PrincipalContext com.oracle.pic.identity"
                                + ".authentication.Principal principal"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context jakarta.ws.rs.core.HttpHeaders httpHeadersContext"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context com.oracle.pic.authproxy.AuthProxyIdentity authProxyIdentity"), is(false));
    }

    @Test
    public void abstractBaseFile_generateWithJaxRsResponse() throws Exception {
        final File abstractResourceFile =
                abstractTestGeneration(
                        "AbstractComputeBaseResource.java",
                        true,
                        true,
                        true,
                        null); // use jaxrs response type
        assertThat(abstractResourceFile.exists(), is(true));
        List<String> linesFromFile = FileUtils.readLines(abstractResourceFile, "UTF-8");

        // Check explicitly for expected jaxrs response
        assertThat(
                find(linesFromFile, "public abstract jakarta.ws.rs.core.Response getBinaryString"), is(true));
        // Check for common values in disabled state.
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.NotNull"), is(true));
        assertThat(find(linesFromFile, "@jakarta.validation.constraints.Pattern"), is(true));
        assertThat(
                find(
                        linesFromFile,
                        "@com.oracle.pic.identity.authorization.sdk.context.PrincipalContext com.oracle.pic.identity"
                                + ".authentication.Principal principal"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context jakarta.ws.rs.core.HttpHeaders httpHeadersContext"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@jakarta.ws.rs.core.Context com.oracle.pic.authproxy.AuthProxyIdentity authProxyIdentity"), is(false));

        File dir = abstractResourceFile.toPath().resolve("../../../../..").normalize().toFile();
        assertNoJavaxInAnyFile(dir);
    }

    @Test
    public void abstractBaseFile_generateWithJaxRsResponse_javax() throws Exception {
        Map<String, String> customAdditionalProperties =
                ImmutableMap.of("useJakartaAnnotations", "false");
        final File abstractResourceFile =
                abstractTestGeneration(
                        "AbstractComputeBaseResource.java",
                        true,
                        true,
                        true,
                        customAdditionalProperties); // use jaxrs response type
        assertThat((abstractResourceFile.exists()), is(true));
        List<String> linesFromFile = FileUtils.readLines(abstractResourceFile, "UTF-8");

        // Check explicitly for expected jaxrs response
        assertThat(
                find(linesFromFile, "public abstract javax.ws.rs.core.Response getBinaryString"), is(true));
        // Check for common values in disabled state.
        assertThat(find(linesFromFile, "@javax.validation.constraints.NotNull"), is(true));
        assertThat(find(linesFromFile, "@javax.validation.constraints.Pattern"), is(true));
        assertThat(
                find(
                        linesFromFile,
                        "@com.oracle.pic.identity.authorization.sdk.context.PrincipalContext com.oracle.pic.identity"
                                + ".authentication.Principal principal"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@javax.ws.rs.core.Context javax.ws.rs.core.HttpHeaders httpHeadersContext"), is(false));
        assertThat(
                find(
                        linesFromFile,
                        "@javax.ws.rs.core.Context com.oracle.pic.authproxy.AuthProxyIdentity authProxyIdentity"), is(false));
    }

    private static void assertNoJavaxInAnyFile(File dir) throws IOException {
        Files.find(
                        dir.toPath(),
                        Integer.MAX_VALUE,
                        (p, a) -> p.toString().endsWith(".java") && a.isRegularFile())
                .forEach(
                        p -> {
                            List<String> lines;
                            try {
                                lines = FileUtils.readLines(p.toFile(), "UTF-8");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            assertThat(find(lines, "javax"), is(false));
                        });
    }

    private static boolean find(List<String> linesFromFile, String contents) {
        return linesFromFile.stream().anyMatch(line -> line.contains(contents));
    }

    private File abstractTestGeneration(
            String fileName,
            boolean useDefaultValues,
            boolean useJaxRsResponse,
            boolean useJakartaAnnotations,
            Map<String, String> customAdditionalProperties)
            throws Exception {
        String testSpecFile = "src/test/resources/reference-spec.yaml";

        assertThat(Files.isDirectory(temp), is(true));
        String outputDir = temp.toString();

        OracleSwaggerMojo mojo = new OracleSwaggerMojo();
        mojo.setOutputDir(outputDir);
        mojo.setBasePackage("com.oracle.gentest");
        mojo.setSpecPath(testSpecFile);
        mojo.setLanguage(OracleJavaHelidonServiceCodegen.LANGUAGE);

        // use the non-default values for everything
        Map<String, String> additionalProperties = new HashMap<>();
        if (!useDefaultValues) {
            for (ConfigOption configOption : ConfigOption.values()) {
                // negate the defaults
                additionalProperties.put(
                        configOption.getAdditionalPropertyKey(),
                        Boolean.valueOf(!configOption.isDefaultValue()).toString());
            }
        }
        // because this is default false, we don't want to negate it unless we're testing this specifically
        if (useJaxRsResponse) {
            additionalProperties.put(
                    ConfigOption.OPTION_USE_JAXRS_SERVICE_RESPONSE.getAdditionalPropertyKey(),
                    Boolean.valueOf(true).toString());
        } else {
            additionalProperties.put(
                    ConfigOption.OPTION_USE_JAXRS_SERVICE_RESPONSE.getAdditionalPropertyKey(),
                    Boolean.valueOf(false).toString());
        }
        if (useJakartaAnnotations) {
            additionalProperties.put(
                    ConfigOption.OPTION_USE_JAKARTA_ANNOTATIONS.getAdditionalPropertyKey(),
                    Boolean.valueOf(true).toString());
        }
        if (customAdditionalProperties != null) {
            additionalProperties.putAll(customAdditionalProperties);
        }

        try {
            mojo.setAdditionalProperties(additionalProperties);
            mojo.execute();
        } catch (MojoExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof PreProcessorException) {
                PreProcessorException e2 = (PreProcessorException) e.getCause();
                for (PreProcessorError error : e2.getErrors()) {
                    System.out.println(
                            error.getStructureName()
                                    + ", "
                                    + error.getPartName()
                                    + ", "
                                    + error.getPartType()
                                    + ", "
                                    + error.getStructureName()
                                    + ", "
                                    + error.getStructureType()
                                    + ", "
                                    + error.getClass());
                }
            }
            throw e;
        }

        String testFilePath =
                Joiner.on(File.separatorChar)
                        .join(
                                outputDir,
                                OracleJavaHelidonServiceCodegen.LANGUAGE + "-sources",
                                "com",
                                "oracle",
                                "gentest",
                                "api",
                                fileName);
        return new File(testFilePath);
    }
}
