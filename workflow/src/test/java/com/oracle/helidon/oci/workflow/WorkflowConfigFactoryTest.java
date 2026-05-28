/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.time.Duration;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static java.util.Map.entry;

class WorkflowConfigFactoryTest {

    @Test
    void createWorkflowConfigWithOldDefaults() {
        WorkflowConfig workflowConfig = new WorkflowConfigFactory(Config.empty()).get();

        assertEquals("localhost", workflowConfig.domainId(), "Default domain id should match the old module");
        assertEquals("http://localhost:39000", workflowConfig.endpointDetails().serverEndpoint(),
                     "Default server endpoint should match the old module");
        assertEquals(Duration.ofSeconds(30), workflowConfig.endpointDetails().connectTimeout(),
                     "Default connect timeout should match the old module");
        assertEquals(Duration.ofSeconds(30), workflowConfig.endpointDetails().workerReadTimeout(),
                     "Default worker read timeout should match the old module");
        assertEquals(Duration.ofSeconds(30), workflowConfig.endpointDetails().pollerReadTimeout(),
                     "Default poller read timeout should match the old module");
        assertFalse(workflowConfig.workerIdentifier().isPresent(),
                    "Worker identifier should be optional so the runtime default can still apply");
        assertFalse(workflowConfig.retryPolicy().isPresent(),
                    "Retry policy should be absent unless explicitly configured");
    }

    @Test
    void createWorkflowConfigWithDurationEndpointPropertiesAndWorkerIdentifierOverride() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("oci.workflow.domain-id", "compute-control-plane"),
                entry("oci.workflow.endpoint-details.server-endpoint",
                      "https://wfaas-overlay.ad2.eu-frankfurt-1.oracleiaas.com"),
                entry("oci.workflow.endpoint-details.connect-timeout", "PT5S"),
                entry("oci.workflow.endpoint-details.worker-read-timeout", "PT45S"),
                entry("oci.workflow.endpoint-details.poller-read-timeout", "PT90S"),
                entry("oci.workflow.worker-identifier", "workflow-worker-1"),
                entry("oci.workflow.retry-policy.max-retry-count", "7"),
                entry("oci.workflow.retry-policy.delay-between-retry", "PT0.25S"),
                entry("oci.workflow.retry-policy.jitter-factor", "0.5"))));

        WorkflowConfig workflowConfig = new WorkflowConfigFactory(config).get();

        assertEquals("compute-control-plane", workflowConfig.domainId(), "Domain id should match");
        assertEquals("https://wfaas-overlay.ad2.eu-frankfurt-1.oracleiaas.com",
                     workflowConfig.endpointDetails().serverEndpoint(),
                     "Server endpoint should match");
        assertEquals(Duration.ofSeconds(5), workflowConfig.endpointDetails().connectTimeout(),
                     "Connect timeout should match");
        assertEquals(Duration.ofSeconds(45), workflowConfig.endpointDetails().workerReadTimeout(),
                     "Worker read timeout should match");
        assertEquals(Duration.ofSeconds(90), workflowConfig.endpointDetails().pollerReadTimeout(),
                     "Poller read timeout should match");
        assertEquals("workflow-worker-1", workflowConfig.workerIdentifier().orElseThrow(),
                     "Worker identifier should match");
        assertEquals(7, workflowConfig.retryPolicy().orElseThrow().maxRetryCount(),
                     "Retry policy max retry count should match");
        assertEquals(Duration.ofMillis(250), workflowConfig.retryPolicy().orElseThrow().delayBetweenRetry(),
                     "Retry policy delay should match");
        assertEquals(0.5d, workflowConfig.retryPolicy().orElseThrow().jitterFactor(),
                     "Retry policy jitter factor should match");
    }

    @Test
    void createWorkflowConfigWithConnectionTimeoutAlias() {
        Config config = Config.just(ConfigSources.create(Map.of(
                "oci.workflow.endpoint-details.connection-timeout", "PT6S"
        )));

        WorkflowConfig workflowConfig = new WorkflowConfigFactory(config).get();

        assertEquals(Duration.ofSeconds(6), workflowConfig.endpointDetails().connectTimeout(),
                     "Connection timeout alias should set connect timeout");
    }

    @Test
    void failWhenConnectTimeoutAndConnectionTimeoutAreConfigured() {
        Config config = Config.just(ConfigSources.create(Map.of(
                "oci.workflow.endpoint-details.connect-timeout", "PT5S",
                "oci.workflow.endpoint-details.connection-timeout", "PT6S"
        )));

        assertThrows(IllegalArgumentException.class, () -> new WorkflowConfigFactory(config).get());
    }
}
