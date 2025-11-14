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

    private static final String ENV_REALM = "realm";
    private static final String ENV_REGION = "region";
    private static final String ENV_REGION_NAME = "region-name";
    private static final String ENV_REGION_INTERNAL_NAME = "region-internal-name";
    private static final String ENV_AVAILABILITY_DOMAIN = "availability-domain";
    private static final String ENV_FAULT_DOMAIN = "fault-domain";
    private static final String ENV_PUBLIC_DOMAIN_NAME = "public-domain-name";
    private static final String ENV_REALM_PUBLIC_DOMAIN_NAME = "realm-public-domain-name";
    private static final String ENV_OCI_PUBLIC_DOMAIN_NAME = "oci-public-domain-name";
    private static final String ENV_IAAS_DOMAIN_NAME = "iaas-domain-name";
    private static final String ENV_REALM_IAAS_DOMAIN_NAME = "realm-iaas-domain-name";
    private static final String ENV_OCI_IAAS_DOMAIN_NAME = "oci-iaas-domain-name";
    private static final String ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME = "first-region-in-realm-public-name";
    private static final String ENV_AD_NUMBER = "ad-number";
    private static final String ENV_NUMBER_FOR_AD = "number-for-ad";
    private static final String ENV_AIRPORT_CODE = "airport-code";
    private static final String ENV_DB_TNS_NAME = "db-tns-name";

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
                        Map.ofEntries(entry(ENV_REALM, "oc1"),
                                      entry(ENV_REGION, "us-ashburn-1"),
                                      entry(ENV_REGION_NAME, "iad"),
                                      entry(ENV_REGION_INTERNAL_NAME, "us-ashburn-1"),
                                      entry(ENV_AVAILABILITY_DOMAIN, "iad-ad-2"),
                                      entry(ENV_FAULT_DOMAIN, "3"),
                                      entry(ENV_PUBLIC_DOMAIN_NAME, "us-ashburn-1.oraclecloud.com"),
                                      entry(ENV_REALM_PUBLIC_DOMAIN_NAME, "oraclecloud.com"),
                                      entry(ENV_OCI_PUBLIC_DOMAIN_NAME, "us-ashburn-1.oci.oraclecloud.com"),
                                      entry(ENV_REALM_IAAS_DOMAIN_NAME, "oracleiaas.com"),
                                      entry(ENV_OCI_IAAS_DOMAIN_NAME, "us-ashburn-1.oci.oracleiaas.com"),
                                      entry(ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME, "us-phoenix-1"),
                                      entry(ENV_IAAS_DOMAIN_NAME, "us-ashburn-1.oracleiaas.com"),
                                      entry(ENV_AD_NUMBER, "ad2"),
                                      entry(ENV_NUMBER_FOR_AD, "2"),
                                      entry(ENV_AIRPORT_CODE, "IAD"),
                                      entry(ENV_DB_TNS_NAME, "usashburn1")
                        )
                ),
                arguments(
                        "custom-mp-meta-config-with-prefix.yaml",
                        "test.prefix",
                        Map.ofEntries(entry(ENV_REALM, "rb5"),
                                      entry(ENV_REGION, "sol-mars-1"),
                                      entry(ENV_REGION_NAME, "sol-mars-1"),
                                      entry(ENV_REGION_INTERNAL_NAME, "sol-mars-1"),
                                      entry(ENV_AVAILABILITY_DOMAIN, "sol-mars-1-ad-1"),
                                      entry(ENV_FAULT_DOMAIN, "5"),
                                      entry(ENV_PUBLIC_DOMAIN_NAME, "sol-mars-1.oraclecloudrb5.com"),
                                      entry(ENV_REALM_PUBLIC_DOMAIN_NAME, "oraclecloudrb5.com"),
                                      entry(ENV_OCI_PUBLIC_DOMAIN_NAME, "sol-mars-1.oci.oraclecloudrb5.com"),
                                      entry(ENV_REALM_IAAS_DOMAIN_NAME, "oraclerealmrb5.com"),
                                      entry(ENV_OCI_IAAS_DOMAIN_NAME, "sol-mars-1.oci.oraclerealmrb5.com"),
                                      entry(ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME, "sol-mars-1"),
                                      entry(ENV_IAAS_DOMAIN_NAME, "sol-mars-1.oraclerealmrb5.com"),
                                      entry(ENV_AD_NUMBER, "ad1"),
                                      entry(ENV_NUMBER_FOR_AD, "1"),
                                      entry(ENV_AIRPORT_CODE, "XXR"),
                                      entry(ENV_DB_TNS_NAME, "solmars1")
                        )
                )
        );
    }
}
