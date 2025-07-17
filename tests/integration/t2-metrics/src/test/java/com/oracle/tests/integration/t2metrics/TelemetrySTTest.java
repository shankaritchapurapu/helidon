/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.t2metrics;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.helidon.integrations.oci.ImdsInstanceInfo;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistry;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.Datapoint;
import com.oracle.bmc.monitoring.model.MetricDataDetails;
import com.oracle.bmc.monitoring.model.PostMetricDataDetails;
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporter;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporterBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TelemetrySTTest {
    private static final String PROJECT = "oci-helidon-reference-demoplane-api-demo";
    private static final String FLEET_T2 = "t2-api";
    private static final String FLEET_MONITORING = "monitoring-sdk";
    private static final String METRIC_NAME = "t2metric";
    private static final Logger LOGGER = Logger.getLogger(TelemetrySTTest.class.getName());

    private static MonitoringClient monitoringClient;
    private static ServiceRegistry registry;

    @BeforeAll
    static void beforeAll() {
        LogConfig.configureRuntime();
        LOGGER.info("Instantiating Authentication Details Provider");

        registry = GlobalServiceRegistry.registry();

        // configured from oci-config.yaml on test classpath
        BasicAuthenticationDetailsProvider adp = registry
                .get(BasicAuthenticationDetailsProvider.class);

        LOGGER.info("Instantiating Monitoring Client");
        monitoringClient = MonitoringClient.builder()
                .configuration(ClientConfiguration.builder()
                                       .connectionTimeoutMillis(3000)
                                       .readTimeoutMillis(3000)
                                       .retryConfiguration(RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION)
                                       .build())
                .build(adp);
        String endpoint = monitoringClient.getEndpoint().replaceFirst("telemetry\\.", "telemetry-ingestion.");
        LOGGER.info("Setting monitoring endpoint to '" + endpoint + "'");
        monitoringClient.setEndpoint(endpoint);
    }

    @Test
    void T2APITest() {
        LOGGER.info("Instantiating TelemetryReporter");

        ImdsInstanceInfo instanceInfo = registry.get(ImdsInstanceInfo.class);
        TelemetryReporter reporter = new TelemetryReporterBuilder()
                .monitoringClient(monitoringClient)
                .project(PROJECT)
                .fleet(FLEET_T2)
                .region(instanceInfo.canonicalRegionName())
                .hostname(instanceInfo.displayName())
                .availabilityDomain(instanceInfo.ociAdName())
                .faultDomain(instanceInfo.faultDomain())
                .build();

        LOGGER.info("Initializing T2 Metrics");
        Metrics.init(reporter);

        LOGGER.info("Sending metrics");
        Metrics.emit(METRIC_NAME, 5);

        LOGGER.info("Sending metrics");
        LOGGER.info("Shutting down T2 Metrics");
        Metrics.shutdown();
    }

    @Test
    void OCIMonitoringSDKTest() {
        LOGGER.info("Creating datapoints and dimensions");
        List<Datapoint> datapoints = List.of(Datapoint.builder()
                                                     .timestamp(Date.from(Instant.now()))
                                                     .value(10.0)
                                                     .count(1)
                                                     .build());
        Map<String, String> dimensions = Map.of("name1", "value1",
                                                "name2", "value2",
                                                "name3", "value3");

        LOGGER.info("Assembling metricDetails");
        MetricDataDetails metricDataDetails =
                MetricDataDetails.builder()
                        // T2 metric compartment for OC1
                        .compartmentId("ocid1.compartment.oc1..aaaaaaaagixeyxsjv643gwx5vf6dkuwmvvf4dlf7k6sobwzbjrtce4lvndwq")
                        .namespace(PROJECT)                      // equivalent to T2 project
                        .resourceGroup(FLEET_MONITORING)         // equivalent to T2 fleet
                        .name(METRIC_NAME)
                        .datapoints(datapoints)
                        .dimensions(dimensions)
                        .build();

        PostMetricDataDetails postMetricDataDetails =
                PostMetricDataDetails.builder().metricData(List.of(metricDataDetails)).build();

        PostMetricDataRequest postMetricDataRequest =
                PostMetricDataRequest.builder()
                        .postMetricDataDetails(postMetricDataDetails)
                        .build();

        LOGGER.info("Posting metrics");
        monitoringClient.postMetricData(postMetricDataRequest);
        LOGGER.info("Metrics successfully sent");
    }
}
