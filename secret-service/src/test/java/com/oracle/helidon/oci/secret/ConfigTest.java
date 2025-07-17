/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.io.StringReader;
import java.util.stream.StreamSupport;

import io.helidon.config.mp.MpConfig;
import io.helidon.config.mp.MpConfigProviderResolver;
import io.helidon.config.yaml.mp.YamlMpConfigSource;

import com.oracle.pic.vault.CacheConfig;
import com.oracle.pic.vault.SecretServiceConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    @Test
    void propNameParsing() {
        SecretServiceMpConfigSource cs = new SecretServiceMpConfigSource();

        assertFalse(cs.parse("oci.vault/helidon1/test-kec").hasPrefix());
        assertTrue(cs.parse("oci.ssv2/helidon2/test-kec").hasPrefix());
        assertTrue(cs.parse("oci.ssv2/secret/helidontest/test-secret/latest").hasPrefix());
        assertEquals("/test-kec", cs.parse("oci.ssv2/test-kec").path());
        assertTrue(cs.parse("oci.ssv2/test-kec").profile().isEmpty());
        assertFalse(cs.parse("%TEST.oci.test/test-kec").hasPrefix());
        assertTrue(cs.parse("%TEST.oci.ssv2/test-kec").hasPrefix());
        assertEquals("/test-kec", cs.parse("%TEST.oci.ssv2/test-kec").path());
        assertTrue(cs.parse("%TEST.oci.ssv2/test-kec").profile().isPresent());

        assertEquals("/secret/helidon/test-secret/latest",
                     cs.parse("oci.ssv2/secret/helidon/test-secret/latest").path());
        assertEquals("/secret/helidon/test-secret/latest",
                     cs.parse("%TEST.oci.ssv2/secret/helidon/test-secret/latest").path());
    }

    @Test
    void lazyInit() {
        assertEquals(StreamSupport.stream(ConfigProvider.getConfig().getConfigSources().spliterator(), false)
                             .filter(cs -> "oci-secret-service".equals(cs.getName())).count(), 2);
    }

    @Test
    void metaConfMapping() {
        var yaml = """
                prefix: oci.ssv2
                endpoint: "https://secret-service-ce.<<region>>.kec.com/v1"
                tlsConfig.caBundle: "/tmp/ca.pem"
                cacheConfig:
                    cacheType: NO_CACHE
                    cacheExpiryInSeconds: 10
                    cacheRefreshIntervalInSeconds: 2
                retryConfig:
                    maxRetries: 3
                    minRetryDelayInMs: 100
                    maxRetryDelayInMs: 200
                """;

        ConfigSource configSource = YamlMpConfigSource.create("application.yaml", new StringReader(yaml));
        var mpConfig = MpConfigProviderResolver.instance().getBuilder()
                .withSources(configSource)
                .addDiscoveredConverters()
                .build();

        SecretServiceMpConfigSource cs = new SecretServiceMpConfigSource(MpConfig.toHelidonConfig(mpConfig), 150);

        SecretServiceConfig secretServiceConfig = cs.client().getSecretServiceConfig();

        assertThat(secretServiceConfig.getEndpoint(), is("https://secret-service-ce.<<region>>.kec.com/v1"));
        assertThat(secretServiceConfig.getCacheConfig().getCacheType(), is(CacheConfig.CacheType.NO_CACHE));

        cs.client().resolveEndpoint("cz-prague-3");

        assertThat(secretServiceConfig.getEndpoint(), is("https://secret-service-ce.cz-prague-3.kec.com/v1"));
        assertThat(secretServiceConfig.getTlsConfig().getCaBundle(), is("/tmp/ca.pem"));
    }
}
