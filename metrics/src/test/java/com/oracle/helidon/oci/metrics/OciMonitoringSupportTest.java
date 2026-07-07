/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URI;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.pic.telemetry.dianoga.MetricTimeSeriesClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OciMonitoringSupportTest {

    @Test
    void endpointFactoryUsesOverlayEndpointOverride() {
        T2MetricsEndpointFactory factory = new T2MetricsEndpointFactory(config("""
                                                                                       metrics:
                                                                                         publishers:
                                                                                           - type: oci
                                                                                             reporter:
                                                                                               overlay:
                                                                                                 endpoint: https://overlay.example.com
                                                                                       """));

        URI endpoint = factory.apply(monitoring("https://telemetry.us-ashburn-1.oraclecloud.com"));

        assertThat(endpoint, is(URI.create("https://overlay.example.com")));
    }

    @Test
    void endpointFactoryIgnoresSubstrateEndpointOverride() {
        T2MetricsEndpointFactory factory = new T2MetricsEndpointFactory(config("""
                                                                                       metrics:
                                                                                         publishers:
                                                                                           - type: oci
                                                                                             reporter:
                                                                                               substrate:
                                                                                                 endpoint: https://substrate.example.com
                                                                                       """));

        URI endpoint = factory.apply(monitoring("https://telemetry.us-ashburn-1.oraclecloud.com"));

        assertThat(endpoint, is(URI.create("https://telemetry-ingestion.us-ashburn-1.oraclecloud.com")));
    }

    @Test
    void monitoringSupplierFailsForSubstrateReporter() {
        OciMonitoringSupplier supplier = new OciMonitoringSupplier(new TestAuthenticationDetailsProvider(),
                                                                   ignored -> URI.create("https://unused.example.com"),
                                                                   config("""
                                                                                  metrics:
                                                                                    publishers:
                                                                                      - type: oci
                                                                                        reporter:
                                                                                          substrate:
                                                                                            project: test-project
                                                                                            fleet: test-fleet
                                                                                  """));

        IllegalStateException exception = assertThrows(IllegalStateException.class, supplier::get);

        assertThat(exception.getMessage(), is("OCI Monitoring client is only available for overlay metrics reporters."));
    }

    @Test
    void metricTimeSeriesClientSupplierUsesSubstrateEndpointOverride() {
        MetricTimeSeriesClient client = mock(MetricTimeSeriesClient.class);
        OciMetricTimeSeriesClientSupplier supplier = new OciMetricTimeSeriesClientSupplier(
                new TestAuthenticationDetailsProvider(),
                config("""
                               metrics:
                                 publishers:
                                   - type: oci
                                     reporter:
                                       substrate:
                                         project: test-project
                                         fleet: test-fleet
                                         endpoint: https://substrate.example.com
                               """),
                (authProvider, clientConfiguration) -> client);

        MetricTimeSeriesClient result = supplier.get();

        assertThat(result, is(client));
        verify(client).setEndpoint("https://substrate.example.com");
    }

    @Test
    void metricTimeSeriesClientSupplierFailsForOverlayReporter() {
        OciMetricTimeSeriesClientSupplier supplier = new OciMetricTimeSeriesClientSupplier(
                new TestAuthenticationDetailsProvider(),
                config("""
                               metrics:
                                 publishers:
                                   - type: oci
                                     reporter:
                                       overlay:
                                         project: test-project
                                         fleet: test-fleet
                               """));

        IllegalStateException exception = assertThrows(IllegalStateException.class, supplier::get);

        assertThat(exception.getMessage(),
                   is("OCI MetricTimeSeriesClient is only available for substrate metrics reporters."));
    }

    private static Config config(String yaml) {
        return Config.just(ConfigSources.create(yaml, MediaTypes.APPLICATION_YAML));
    }

    private static Monitoring monitoring(String endpoint) {
        return (Monitoring) Proxy.newProxyInstance(OciMonitoringSupportTest.class.getClassLoader(),
                                                  new Class<?>[] {Monitoring.class},
                                                  (proxy, method, args) -> switch (method.getName()) {
                                                      case "getEndpoint" -> endpoint;
                                                      case "toString" -> "test-monitoring";
                                                      default -> throw new UnsupportedOperationException(method.getName());
                                                  });
    }

    private static final class TestAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {
        @Override
        public String getKeyId() {
            return "test-key";
        }

        @Override
        public InputStream getPrivateKey() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public String getPassPhrase() {
            return null;
        }

        @Override
        public char[] getPassphraseCharacters() {
            return null;
        }
    }
}
