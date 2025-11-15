/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow.client;

import io.helidon.microprofile.testing.AddConfig;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import io.helidon.config.Config;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static com.oracle.helidon.oci.workflow.client.OciWorkflowClientCdiBeanTest.OCI_WORKFLOW_DOMAIN_ID_KEY;
import static com.oracle.helidon.oci.workflow.client.OciWorkflowClientCdiBeanTest.OCI_WORKFLOW_SERVER_ENDPOINT_KEY;

@HelidonTest
@AddConfig(key = OCI_WORKFLOW_DOMAIN_ID_KEY, value = "test-domain")
@AddConfig(key = OCI_WORKFLOW_SERVER_ENDPOINT_KEY, value = "https://workflow.server-endpoint.com")
public class OciWorkflowClientCdiBeanTest {
    static final String OCI_WORKFLOW_DOMAIN_ID_KEY = "oci.workflow.domain-id";
    static final String OCI_WORKFLOW_SERVER_ENDPOINT_KEY = "oci.workflow.endpoint-details.server-endpoint";
    private static final String DEFAULT_OCI_WORKFLOW_SERVER_ENDPOINT = "http://localhost:39000";
    private static final String DEFAULT_OCI_WORKFLOW_DOMAIN_ID = "localhost";

    @Inject
    OciWorkflowClient workflowClientBean;

    @Inject
    Config config;

    @Test
    public void testWorkflowClient() {
        var workerClient = workflowClientBean.workerClient();
        var pollerClient = workflowClientBean.pollerClient();

        assertThat(workerClient, not(nullValue()));
        assertThat(pollerClient, not(nullValue()));
        assertThat(workerClient, not(pollerClient));

        String domainId = config.get(OCI_WORKFLOW_DOMAIN_ID_KEY).asString().orElseThrow();
        String serverEndpoint = config.get(OCI_WORKFLOW_SERVER_ENDPOINT_KEY).asString().orElseThrow();
        assertThat(workerClient.getDomainId(), is(domainId == null ? DEFAULT_OCI_WORKFLOW_DOMAIN_ID : domainId));
        WorkflowClientConfig workflowClientConfig = workflowClientBean.workflowClientConfig();
        assertThat(workflowClientConfig.domainId(), is(workerClient.getDomainId()));
        assertThat(workflowClientConfig.endpointDetails().serverEndpoint(),
                   is(serverEndpoint == null ? DEFAULT_OCI_WORKFLOW_SERVER_ENDPOINT : serverEndpoint));
    }
}
