/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import io.helidon.config.ConfigException;

import com.oracle.pic.commons.util.CoreRegionsRegistrarHelper;
import com.oracle.pic.commons.util.Region;

final class DynamicCoreRegions {
    private static final System.Logger LOGGER = System.getLogger(DynamicCoreRegions.class.getName());
    // OCI dynamic core-regions guidance documents these RPM-delivered default metadata paths for
    // startup/manual import, and Helidon mirrors them so oci-env follows the same runtime
    // convention used by Pegasus services:
    // https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/torb/touchless-region-build-cookbooks/onboarding-to-dynamic-core-regions.htm#setting-up-automatic-imports-for-dropwizard
    // https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/torb/touchless-region-build-cookbooks/onboarding-to-dynamic-core-regions.htm#call-the-import-with-override-method
    static final String DEFAULT_IMPORT_PATH =
            "/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata.json";
    static final String DEFAULT_IMPORT_OVERRIDE_PATH =
            "/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata_override.json";
    static final String ENABLED_KEY = "dynamic-core-regions.enabled";
    // OCI dynamic core-regions guidance recommends validating import with Doha because it is intended
    // to prove that imported metadata, not just the built-in region catalog, is being used:
    // https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/torb/touchless-region-build-cookbooks/onboarding-to-dynamic-core-regions.htm#validating-metadata-import
    static final String DEFAULT_VALIDATION_REGION = "me-dcc-doha-1";

    private final OciEnvDynamicCoreRegions config;
    DynamicCoreRegions(OciEnvDynamicCoreRegions config) {
        this.config = Objects.requireNonNull(config);
    }

    void importIfConfigured() {
        if (!config.enabled()) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Dynamic core-regions import is disabled");
            }
            return;
        }

        Path importPath = config.importPath();
        boolean importPathExists = Files.isRegularFile(importPath);
        Optional<Path> overridePath = existingOverridePath();

        if (!importPathExists && overridePath.isEmpty()) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Skipping dynamic core-regions import because metadata file ''{0}'' and override file ''{1}''"
                                   + " do not exist",
                           importPath,
                           config.importOverridePath());
            }
            return;
        }

        if (overridePath.isEmpty()) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Importing dynamic core-regions metadata from ''{0}'' without override file",
                           importPath);
            }
            CoreRegionsRegistrarHelper.importDataFromJsonPath(importPath);
        } else {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           importPathExists
                                   ? "Importing dynamic core-regions metadata from ''{0}'' with override ''{1}''"
                                   : "Importing dynamic core-regions metadata from override ''{1}'' because default "
                                           + "metadata file ''{0}'' does not exist",
                           importPath,
                           overridePath.get());
            }
            CoreRegionsRegistrarHelper.importDataFromJsonPathWithOverride(importPath, overridePath.get());
        }
        if (config.validateImport()) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Validating dynamic core-regions import using region ''{0}''",
                           config.validationRegion());
            }
            validateImport(config.validationRegion());
        } else if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG, "Dynamic core-regions import validation is disabled");
        }
    }

    Optional<Path> existingOverridePath() {
        Path importOverridePath = config.importOverridePath();
        return Files.isRegularFile(importOverridePath)
                ? Optional.of(importOverridePath)
                : Optional.empty();
    }

    private static void validateImport(String validationRegion) {
        try {
            Region.fromPublicRegionName(validationRegion);
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG,
                           "Dynamic core-regions import validation succeeded for region ''{0}''",
                           validationRegion);
            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(String.format("Dynamic core-regions import validation failed for region '%s'",
                                                   validationRegion),
                                      e);
        }
    }
}
