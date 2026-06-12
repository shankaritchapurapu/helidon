/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.tests.integration.workflow;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;

import com.oracle.bmc.model.BmcException;
import com.oracle.jipher.provider.JipherJCE;
import com.oracle.pic.workflow.client.v1.model.LaunchWorkflowArguments;
import com.oracle.pic.workflow.client.v1.model.StepDefinition;
import com.oracle.pic.workflow.client.v1.model.WorkflowDefinition;
import com.oracle.pic.workflow.client.v1.model.WorkflowDefinitionId;
import com.oracle.pic.workflow.client.v1.model.WorkflowInstance;
import com.oracle.pic.workflow.worker.PagedResult;
import com.oracle.pic.workflow.worker.WorkflowClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class WorkflowIT {
    private static final String WFAAS_DOMAIN_ID = "helidon-wfaas-instance.wfaas-overlay.ad1.r2.oracleiaas.com";
    private static final String WORKFLOW_NAME = "workflow-name";
    private static final String WORKFLOW_STEP = "workflow-step";

    @BeforeAll
    static void beforeAll() {
        System.setProperty("useJipherJceProvider", "true");
        Security.addProvider(new JipherJCE());
        LogConfig.configureRuntime();
    }

    @Test
    void listsWorkflowInstances() {
        WorkflowClient client = Services.get(WorkflowClient.class);
        assertThat(client.getDomainId(), is(WFAAS_DOMAIN_ID));

        PagedResult<WorkflowInstance> workflowInstances = client.getWorkflowInstances(true, true, 1, null);
        assertThat(workflowInstances, notNullValue());

        List<WorkflowInstance> items = workflowInstances.getResult();
        assertThat(items, notNullValue());
        assertThat(items.size() <= 1, is(true));
    }

    @Test
    void launchesWorkflow() {
        WorkflowClient client = Services.get(WorkflowClient.class);
        assertThat(client.getDomainId(), is(WFAAS_DOMAIN_ID));

        WorkflowDefinitionId definitionId = WorkflowDefinitionId.builder()
                .name(WORKFLOW_NAME)
                .majorVersion(1)
                .minorVersion(0)
                .build();

        createWorkflowDefinition(client, definitionId);

        String testId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
        LaunchWorkflowArguments arguments = LaunchWorkflowArguments.builder()
                .workflowDefinitionId(definitionId)
                .workflowArguments(("{}").getBytes(StandardCharsets.UTF_8))
                .surrogateKey(testId)
                .tag(testId)
                .build();

        String workflowInstanceId = null;
        try {
            // Launch the workflow using the created definition
            WorkflowInstance launchedWorkflowInstance = client.launchWorkflow(arguments, testId);
            workflowInstanceId = launchedWorkflowInstance.getId();

            assertThat("Workflow instance id", workflowInstanceId, notNullValue());
            assertThat("Workflow instance id", workflowInstanceId.isBlank(), is(false));
            assertThat(launchedWorkflowInstance.getStatus(), notNullValue());
            assertThat(launchedWorkflowInstance.getWorkflowDefinitionId(), notNullValue());
            assertThat(launchedWorkflowInstance.getWorkflowDefinitionId().getName(), is(WORKFLOW_NAME));
            assertThat(launchedWorkflowInstance.getWorkflowDefinitionId().getMajorVersion(), is(1));
            assertThat(launchedWorkflowInstance.getWorkflowDefinitionId().getMinorVersion(), is(0));
            assertThat(launchedWorkflowInstance.getSurrogateKey(), is(testId));
            assertThat(launchedWorkflowInstance.getTag(), is(testId));

            // Get the workflow by its id
            WorkflowInstance fetchedWorkflowInstance = client.getWorkflowInstance(workflowInstanceId);
            assertThat(fetchedWorkflowInstance.getId(), is(workflowInstanceId));
            assertThat(fetchedWorkflowInstance.getStatus(), notNullValue());
        } finally {
            // Ensure cancellation of workflow instance
            if (workflowInstanceId != null) {
                client.cancelWorkflowInstance(workflowInstanceId);
            }
        }
    }

    private void createWorkflowDefinition(WorkflowClient client, WorkflowDefinitionId id) {
        try {
            Optional<WorkflowDefinition> definition = client.getWorkflowDefinition(id);
            assertThat(definition.isPresent(), is(true));
            assertThat(definition.get().getFirstStep(), is(WORKFLOW_STEP));
        } catch (BmcException e) {
            // If there is no workflow definition, create one.
            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .id(id)
                    .firstStep(WORKFLOW_STEP)
                    .approxMaximumRunTimeSeconds(120L)
                    .approxStartTimeoutSeconds(120L)
                    .steps(List.of(StepDefinition.builder()
                            .name(WORKFLOW_STEP)
                            .assignmentTTLMillis(30_000L)
                            .leaseTTLMillis(30_000L)
                            .numAssignmentsBeforeFailure(1)
                            .build()))
                    .build();
            client.postWorkflowDefinition(definition);
        }
    }
}
