package com.oracle.test.t2metics;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.Datapoint;
import com.oracle.bmc.monitoring.model.MetricDataDetails;
import com.oracle.bmc.monitoring.model.PostMetricDataDetails;
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest;
import com.oracle.bmc.retrier.RetryConfiguration;

import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporter;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporterBuilder;
import com.oracle.pic.telemetry.overlay.clients.InstanceMetadata;
import com.oracle.pic.telemetry.overlay.clients.OverlayInstanceMetadataClient;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

class TelemetrySTTest {
    private static final String PROJECT = "xxxx";
    private static final String FLEET_T2 = "t2-api";
    private static final String FLEET_MONITORING = "monitoring-sdk";
    private static final String METRIC_NAME = "t2metric";
    private static final String INSTANCE_METADATA_BASE_URL = "http://localhost:8000/opc/v2/";
    private static final String INSTANCE_METADATA_INSTANCE_URL = INSTANCE_METADATA_BASE_URL + "instance/";
    private static MonitoringClient monitoringClient;
    private static String requestId;
    public enum InstanceMetadataProperty {
        /*
         * The name should match metadata property name here https://docs.cloud.oracle.com/iaas/Content/Compute/Tasks/gettingmetadata.htm
         */
        DISPLAY_NAME("displayName"),
        COMPARTMENT_ID("compartmentId"),
        CANONICAL_REGION_NAME("canonicalRegionName"),
        OCI_AD_NAME("ociAdName"),
        FAULT_DOMAIN("faultDomain"),
        HOST_NAME("hostname");
        String name;
        InstanceMetadataProperty(String name) {
            this.name = name;
        }
    }
    private static final Logger LOGGER = Logger.getLogger(TelemetrySTTest.class.getName());

    @BeforeAll
    static void beforeAll() {
        LOGGER.info("Instantiating Instance Principal");
        InstancePrincipalsAuthenticationDetailsProvider instancePrincipalAuth =
                InstancePrincipalsAuthenticationDetailsProvider.builder()
                        .metadataBaseUrl(INSTANCE_METADATA_BASE_URL)
                        .detectEndpointRetries(1)
                        .timeoutForEachRetry(3000)
                        .build();
        LOGGER.info("Instantiating Monitoring Client");
        monitoringClient = MonitoringClient.builder()
                .configuration(ClientConfiguration.builder()
                                       .connectionTimeoutMillis(3000)
                                       .readTimeoutMillis(3000)
                                       .retryConfiguration(RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION)
                                       .build())
                .build(instancePrincipalAuth);
        String endpoint = monitoringClient.getEndpoint().replaceFirst("telemetry\\.", "telemetry-ingestion.");
        LOGGER.info("Setting monitoring endpoint to '" + endpoint + "'");
        monitoringClient.setEndpoint(endpoint);
    }

    @Test
    void T2APITest() {
        LOGGER.info("Instantiating TelemetryReporter");
        TelemetryReporter reporter = new TelemetryReporterBuilder()
                .monitoringClient(monitoringClient)
                .project(PROJECT)
                .fleet(FLEET_T2)
                // .instanceMetadataEndpointOverride(INSTANCE_METADATA_INSTANCE_URL)
                // .useMetadataService(true)
                .region(getFieldValueByName(InstanceMetadataProperty.CANONICAL_REGION_NAME))
                .hostname(getFieldValueByName(InstanceMetadataProperty.DISPLAY_NAME))
                .availabilityDomain(getFieldValueByName(InstanceMetadataProperty.OCI_AD_NAME))
                .faultDomain(getFieldValueByName(InstanceMetadataProperty.FAULT_DOMAIN))
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

        LOGGER.info("Assembing metricDetails");
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

    // Used to extract a field from Instance Metadata Service
    private static String getFieldValueByName(InstanceMetadataProperty field) {
        String uri = String.format(INSTANCE_METADATA_BASE_URL + "instance/%s", field.name);
        try {
            Client client = ClientBuilder.newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .build();
            Response response = client.target(uri).request().header("Authorization", "Bearer Oracle").get();
            String requestId = response.readEntity(String.class);
            return requestId;
        } catch (Exception exc) {
            System.out.print("Failed to get " + field.name + " from Instance Metadata Service: " + exc);
        }
        return null;
    }

    // private InstanceMetadata getInstanceMetadata() {
    //     OverlayInstanceMetadataClient metadataClient =
    //             new OverlayInstanceMetadataClient(INSTANCE_METADATA_INSTANCE_URL);
    //     return metadataClient.getInstanceMetadata();
    // }
}
