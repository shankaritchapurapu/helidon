/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow.client;

import io.helidon.config.Config;
import io.helidon.microprofile.testing.AddConfig;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import io.helidon.service.registry.Service;

import com.oracle.pic.workflow.worker.WorkflowClient;
import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.workflow.client.OciWorkflowClientCdiBeanTest.OCI_WORKFLOW_DOMAIN_ID_KEY;
import static com.oracle.helidon.oci.workflow.client.OciWorkflowClientCdiBeanTest.OCI_WORKFLOW_SERVER_ENDPOINT_KEY;
import static com.oracle.pic.workflow.module.WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME;
import static com.oracle.pic.workflow.module.WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonTest
@AddConfig(key = OCI_WORKFLOW_DOMAIN_ID_KEY, value = "test-domain")
@AddConfig(key = OCI_WORKFLOW_SERVER_ENDPOINT_KEY, value = "https://workflow.server-endpoint.com")
public class OciWorkflowClientDeclarativeTest {
    static final String OCI_WORKFLOW_DOMAIN_ID_KEY = "oci.workflow.domain-id";
    static final String OCI_WORKFLOW_SERVER_ENDPOINT_KEY = "oci.workflow.endpoint-details.server-endpoint";
    private static final String DEFAULT_OCI_WORKFLOW_SERVER_ENDPOINT = "http://localhost:39000";
    private static final String DEFAULT_OCI_WORKFLOW_DOMAIN_ID = "localhost";
    private final Config config;
    private final OciWorkflowClient workflowClientBean;
    private final WorkflowClient workerClient;
    private final WorkflowClient pollerClient;

    @Service.Inject
    OciWorkflowClientDeclarativeTest(Config config,
                                     OciWorkflowClient ociWorkflowClient,
                                     @Service.Named(WORKFLOW_WORKER_CLIENT_NAME) WorkflowClient workerClient,
                                     @Service.Named(WORKFLOW_POLLER_CLIENT_NAME) WorkflowClient pollerClient) {
        this.config = config;
        this.workflowClientBean = ociWorkflowClient;
        this.workerClient = workerClient;
        this.pollerClient = pollerClient;
    }

    @Test
    public void testWorkflowClient() {
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
