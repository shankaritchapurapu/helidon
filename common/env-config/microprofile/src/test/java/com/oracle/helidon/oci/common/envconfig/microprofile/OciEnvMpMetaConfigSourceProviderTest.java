/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.envconfig.microprofile;

import java.util.Map;
import java.util.stream.Stream;

import io.helidon.logging.common.LogConfig;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class OciEnvMpMetaConfigSourceProviderTest {
    private static final String META_CONFIG_SYSTEM_PROPERTY = "io.helidon.config.mp.meta-config";

    private static ConfigProviderResolver configResolver;
    private Config config;

    @BeforeAll
    static void getProviderResolver() {
        LogConfig.configureRuntime();
        configResolver = ConfigProviderResolver.instance();
    }

    @AfterAll
    static void resetSystemProperties() {
        System.clearProperty(META_CONFIG_SYSTEM_PROPERTY);
    }

    @BeforeEach
    void resetConfig() {
        if (config == null) {
            // first run - need to remove existing props
            System.clearProperty(META_CONFIG_SYSTEM_PROPERTY);
            configResolver.releaseConfig(ConfigProvider.getConfig());
        } else {
            configResolver.releaseConfig(config);
            config = null;
        }
    }

    @ParameterizedTest
    @MethodSource("envConfigMapParam")
    void testEnvironmentConfig(String metaConfigFileName, String prefix, Map<String, Object> envConfig) {
        if (metaConfigFileName != null) {
            System.setProperty(META_CONFIG_SYSTEM_PROPERTY, metaConfigFileName);
        }
        config = ConfigProvider.getConfig();
        // Iterate and validate each key
        envConfig.forEach((key, value) ->
                                  assertThat(config.getValue(prefix + "." + key, String.class), is(envConfig.get(key))));

    }

    // Each argument will contain a meta-config filename, a prefix, and the expected configuration values in a Map
    private static Stream<Arguments> envConfigMapParam() {
        return Stream.of(
                arguments(
                        null, // will use the default mp-meta-config.yaml
                        "oci.env",  // Default value if prefix is not set
                        Map.ofEntries(entry(OciEnvMpConfigSource.ENV_REALM, "oc1"),
                                      entry(OciEnvMpConfigSource.ENV_REGION, "us-ashburn-1"),
                                      entry(OciEnvMpConfigSource.ENV_REGION_NAME, "iad"),
                                      entry(OciEnvMpConfigSource.ENV_REGION_INTERNAL_NAME, "us-ashburn-1"),
                                      entry(OciEnvMpConfigSource.ENV_AVAILABILITY_DOMAIN, "iad-ad-2"),
                                      entry(OciEnvMpConfigSource.ENV_FAULT_DOMAIN, "3"),
                                      entry(OciEnvMpConfigSource.ENV_PUBLIC_DOMAIN_NAME, "us-ashburn-1.oraclecloud.com"),
                                      entry(OciEnvMpConfigSource.ENV_REALM_PUBLIC_DOMAIN_NAME, "oraclecloud.com"),
                                      entry(OciEnvMpConfigSource.ENV_OCI_PUBLIC_DOMAIN_NAME, "us-ashburn-1.oci.oraclecloud.com"),
                                      entry(OciEnvMpConfigSource.ENV_REALM_IAAS_DOMAIN_NAME, "oracleiaas.com"),
                                      entry(OciEnvMpConfigSource.ENV_OCI_IAAS_DOMAIN_NAME, "us-ashburn-1.oci.oracleiaas.com"),
                                      entry(OciEnvMpConfigSource.ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME, "us-phoenix-1"),
                                      entry(OciEnvMpConfigSource.ENV_IAAS_DOMAIN_NAME, "us-ashburn-1.oracleiaas.com"),
                                      entry(OciEnvMpConfigSource.ENV_AD_NUMBER, "ad2"),
                                      entry(OciEnvMpConfigSource.ENV_NUMBER_FOR_AD, "2"),
                                      entry(OciEnvMpConfigSource.ENV_AIRPORT_CODE, "IAD"),
                                      entry(OciEnvMpConfigSource.ENV_DB_TNS_NAME, "usashburn1")
                        )
                ),
                arguments(
                        "custom-mp-meta-config-with-prefix.yaml",
                        "test.prefix",
                        Map.ofEntries(entry(OciEnvMpConfigSource.ENV_REALM, "rb5"),
                                      entry(OciEnvMpConfigSource.ENV_REGION, "sol-mars-1"),
                                      entry(OciEnvMpConfigSource.ENV_REGION_NAME, "sol-mars-1"),
                                      entry(OciEnvMpConfigSource.ENV_REGION_INTERNAL_NAME, "sol-mars-1"),
                                      entry(OciEnvMpConfigSource.ENV_AVAILABILITY_DOMAIN, "sol-mars-1-ad-1"),
                                      entry(OciEnvMpConfigSource.ENV_FAULT_DOMAIN, "5"),
                                      entry(OciEnvMpConfigSource.ENV_PUBLIC_DOMAIN_NAME, "sol-mars-1.oraclecloudrb5.com"),
                                      entry(OciEnvMpConfigSource.ENV_REALM_PUBLIC_DOMAIN_NAME, "oraclecloudrb5.com"),
                                      entry(OciEnvMpConfigSource.ENV_OCI_PUBLIC_DOMAIN_NAME, "sol-mars-1.oci.oraclecloudrb5.com"),
                                      entry(OciEnvMpConfigSource.ENV_REALM_IAAS_DOMAIN_NAME, "oraclerealmrb5.com"),
                                      entry(OciEnvMpConfigSource.ENV_OCI_IAAS_DOMAIN_NAME, "sol-mars-1.oci.oraclerealmrb5.com"),
                                      entry(OciEnvMpConfigSource.ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME, "sol-mars-1"),
                                      entry(OciEnvMpConfigSource.ENV_IAAS_DOMAIN_NAME, "sol-mars-1.oraclerealmrb5.com"),
                                      entry(OciEnvMpConfigSource.ENV_AD_NUMBER, "ad1"),
                                      entry(OciEnvMpConfigSource.ENV_NUMBER_FOR_AD, "1"),
                                      entry(OciEnvMpConfigSource.ENV_AIRPORT_CODE, "XXR"),
                                      entry(OciEnvMpConfigSource.ENV_DB_TNS_NAME, "solmars1")
                        )
                )
        );
    }
}
