/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.envconfig;

import java.util.Map;

import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;

import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OciEnvConfigSourceProviderTest {
    private static final String PREFIX = "test.prefix";
    private static final Map<String, Object> ENV_CONFIG_VALUES_MAP =
            Map.ofEntries(entry(OciEnvConfigSource.ENV_REALM, "rb5"),
                          entry(OciEnvConfigSource.ENV_REGION, "sol-mars-1"),
                          entry(OciEnvConfigSource.ENV_REGION_NAME, "sol-mars-1"),
                          entry(OciEnvConfigSource.ENV_REGION_INTERNAL_NAME, "sol-mars-1"),
                          entry(OciEnvConfigSource.ENV_AVAILABILITY_DOMAIN, "sol-mars-1-ad-1"),
                          entry(OciEnvConfigSource.ENV_FAULT_DOMAIN, "5"),
                          entry(OciEnvConfigSource.ENV_PUBLIC_DOMAIN_NAME, "sol-mars-1.oraclecloudrb5.com"),
                          entry(OciEnvConfigSource.ENV_REALM_PUBLIC_DOMAIN_NAME, "oraclecloudrb5.com"),
                          entry(OciEnvConfigSource.ENV_OCI_PUBLIC_DOMAIN_NAME, "sol-mars-1.oci.oraclecloudrb5.com"),
                          entry(OciEnvConfigSource.ENV_REALM_IAAS_DOMAIN_NAME, "oraclerealmrb5.com"),
                          entry(OciEnvConfigSource.ENV_OCI_IAAS_DOMAIN_NAME, "sol-mars-1.oci.oraclerealmrb5.com"),
                          entry(OciEnvConfigSource.ENV_FIRST_REGION_IN_REALM_PUBLIC_NAME, "sol-mars-1"),
                          entry(OciEnvConfigSource.ENV_IAAS_DOMAIN_NAME, "sol-mars-1.oraclerealmrb5.com"),
                          entry(OciEnvConfigSource.ENV_AD_NUMBER, "ad1"),
                          entry(OciEnvConfigSource.ENV_NUMBER_FOR_AD, "1"),
                          entry(OciEnvConfigSource.ENV_AIRPORT_CODE, "XXR"),
                          entry(OciEnvConfigSource.ENV_DB_TNS_NAME, "solmars1")
            );

    @Test
    void name() {
        LogConfig.configureRuntime();
        var config = Services.get(Config.class);
        ENV_CONFIG_VALUES_MAP.forEach((key, value) -> assertThat(config.get(PREFIX + "." + key).asString().get(),
                                                                 is(ENV_CONFIG_VALUES_MAP.get(key))));
    }
}
