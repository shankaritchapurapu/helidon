/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DynamicCoreRegionsTest {

    @TempDir
    Path tempDir;

    @Test
    void doesNothingWhenImportIsDisabledAndIgnoresMissingOverridePath() throws Exception {
        Path importPath = tempDir.resolve("rbcp_core_regions_metadata.json");
        Path importOverridePath = tempDir.resolve("rbcp_core_regions_metadata_override.json");
        Files.writeString(importPath, "{\"ignored\":true}");

        Config disabledConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry(DynamicCoreRegions.ENABLED_KEY, "false"),
                entry("dynamic-core-regions.import-path", importPath.toString()),
                entry("dynamic-core-regions.import-override-path", importOverridePath.toString()))));
        DynamicCoreRegions disabledDynamicCoreRegions = new DynamicCoreRegions(dynamicCoreRegions(disabledConfig));

        assertDoesNotThrow(disabledDynamicCoreRegions::importIfConfigured);

        Config enabledConfig = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.import-path", importPath.toString()),
                entry("dynamic-core-regions.import-override-path", importOverridePath.toString()))));
        DynamicCoreRegions enabledDynamicCoreRegions = new DynamicCoreRegions(dynamicCoreRegions(enabledConfig));

        assertThat(enabledDynamicCoreRegions.existingOverridePath().isEmpty(), is(true));
    }

    @Test
    void usesOverrideWhenOverridePathPresent() throws Exception {
        Path importPath = tempDir.resolve("rbcp_core_regions_metadata.json");
        Path importOverridePath = tempDir.resolve("rbcp_core_regions_metadata_override.json");
        Files.writeString(importPath, "{\"ignored\":true}");
        Files.writeString(importOverridePath, "{\"override\":true}");

        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.import-path", importPath.toString()),
                entry("dynamic-core-regions.import-override-path", importOverridePath.toString()))));

        DynamicCoreRegions dynamicCoreRegions = new DynamicCoreRegions(dynamicCoreRegions(config));

        assertThat(dynamicCoreRegions.existingOverridePath().orElseThrow(), is(importOverridePath));
    }

    @Test
    void importsOverrideMetadataWhenPrimaryImportPathMissing() throws Exception {
        String importedRegion = "zz-codex-override-only-1";
        Path importPath = tempDir.resolve("rbcp_core_regions_metadata.json");
        Path importOverridePath = tempDir.resolve("rbcp_core_regions_metadata_override.json");
        Files.writeString(importOverridePath, regionMetadata(importedRegion, "QZX", "qx", "Codex Override Only"));

        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.import-path", importPath.toString()),
                entry("dynamic-core-regions.import-override-path", importOverridePath.toString()),
                entry("dynamic-core-regions.validation-region", importedRegion))));

        DynamicCoreRegions dynamicCoreRegions = new DynamicCoreRegions(dynamicCoreRegions(config));

        assertDoesNotThrow(dynamicCoreRegions::importIfConfigured);
        assertThat(Region.fromPublicRegionName(importedRegion).getPublicRegionName(), is(importedRegion));
    }

    @Test
    void importsPrimaryMetadataWhenOverrideMissing() throws Exception {
        String importedRegion = "zz-codex-primary-only-1";
        Path importPath = tempDir.resolve("rbcp_core_regions_metadata.json");
        Path importOverridePath = tempDir.resolve("rbcp_core_regions_metadata_override.json");
        Files.writeString(importPath, regionMetadata(importedRegion, "QZY", "qy", "Codex Primary Only"));

        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.import-path", importPath.toString()),
                entry("dynamic-core-regions.import-override-path", importOverridePath.toString()),
                entry("dynamic-core-regions.validation-region", importedRegion))));

        DynamicCoreRegions dynamicCoreRegions = new DynamicCoreRegions(dynamicCoreRegions(config));

        assertDoesNotThrow(dynamicCoreRegions::importIfConfigured);
        assertThat(Region.fromPublicRegionName(importedRegion).getPublicRegionName(), is(importedRegion));
    }

    @Test
    void skipsImportWhenImportPathMissing() {
        Path importPath = tempDir.resolve("rbcp_core_regions_metadata.json");
        Path importOverridePath = tempDir.resolve("rbcp_core_regions_metadata_override.json");

        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.import-path", importPath.toString()),
                entry("dynamic-core-regions.import-override-path", importOverridePath.toString()))));

        DynamicCoreRegions dynamicCoreRegions = new DynamicCoreRegions(dynamicCoreRegions(config));

        assertDoesNotThrow(dynamicCoreRegions::importIfConfigured);
    }

    @Test
    void validatesImportByDefault() {
        OciEnvDynamicCoreRegions dynamicCoreRegions = dynamicCoreRegions(Config.empty());

        assertThat(dynamicCoreRegions.enabled(), is(true));
        assertThat(dynamicCoreRegions.validationRegion(), is(DynamicCoreRegions.DEFAULT_VALIDATION_REGION));
        assertThat(dynamicCoreRegions.validateImport(), is(true));
    }

    @Test
    void allowsConfiguringValidationRegion() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.validation-region", "ap-chiyoda-1"))));

        OciEnvDynamicCoreRegions dynamicCoreRegions = dynamicCoreRegions(config);

        assertThat(dynamicCoreRegions.validationRegion(), is("ap-chiyoda-1"));
    }

    @Test
    void allowsDisablingImportValidation() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("dynamic-core-regions.validate-import", "false"))));

        OciEnvDynamicCoreRegions dynamicCoreRegions = dynamicCoreRegions(config);

        assertThat(dynamicCoreRegions.validateImport(), is(false));
    }

    @Test
    void allowsDisablingImportExplicitly() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry(DynamicCoreRegions.ENABLED_KEY, "false"))));

        OciEnvDynamicCoreRegions dynamicCoreRegions = dynamicCoreRegions(config);

        assertThat(dynamicCoreRegions.enabled(), is(false));
    }

    private static OciEnvDynamicCoreRegions dynamicCoreRegions(Config config) {
        return OciEnvConfig.create(config).dynamicCoreRegions().orElseGet(OciEnvDynamicCoreRegions::create);
    }

    private static String regionMetadata(String importedRegion,
                                         String airportCode,
                                         String adIdCodePrefix,
                                         String displayName) {
        return """
                {
                  "region": {
                    "publicRegionName": "%1$s",
                    "airportCode": "%2$s",
                    "realmName": "oc1",
                    "internalName": "%1$s",
                    "name": "%1$s",
                    "displayName": "%4$s",
                    "geoRegion": "NORTH_AMERICA_REGION",
                    "adIdCodePrefix": "%3$s",
                    "countryName": "United States",
                    "tzDataName": "America/Los_Angeles",
                    "serviceMetadata": {}
                  }
                }
                """.formatted(importedRegion, airportCode, adIdCodePrefix, displayName);
    }
}
