/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.workflow;

import java.nio.charset.StandardCharsets;

import io.helidon.service.registry.Service;
import io.helidon.json.binding.JsonBinding;

import com.oracle.pic.workflow.client.v1.model.LaunchWorkflowArguments;
import com.oracle.pic.workflow.client.v1.model.WorkflowDefinitionId;
import com.oracle.pic.workflow.client.v1.model.WorkflowInstance;
import com.oracle.pic.workflow.worker.WorkflowClient;

/**
 * Small application service that hides WFaaS request construction from the HTTP endpoint.
 */
@Service.Singleton
class ProvisioningWorkflowService {
    private static final String WORKFLOW_NAME = "instance-create";
    private static final int WORKFLOW_MAJOR_VERSION = 3;
    private static final int WORKFLOW_MINOR_VERSION = 1;
    private static final JsonBinding JSON_BINDING = JsonBinding.create();

    private final WorkflowClient workflowClient;

    @Service.Inject
    ProvisioningWorkflowService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    WorkflowSnapshot launch(String resourceId, boolean simulateFailure) {
        LaunchWorkflowArguments launchRequest = LaunchWorkflowArguments.builder()
                .workflowDefinitionId(workflowDefinitionId())
                .workflowArguments(serializeArguments(new LaunchWorkflowRequest(resourceId, simulateFailure)))
                .build();
        return snapshot(workflowClient.launchWorkflow(launchRequest));
    }

    WorkflowSnapshot get(String workflowInstanceId) {
        return snapshot(workflowClient.getWorkflowInstance(workflowInstanceId));
    }

    private static WorkflowSnapshot snapshot(WorkflowInstance instance) {
        return new WorkflowSnapshot(instance.getId(),
                                    instance.getStatus().name(),
                                    instance.getTag() == null ? "" : instance.getTag());
    }

    private static WorkflowDefinitionId workflowDefinitionId() {
        return WorkflowDefinitionId.builder()
                .name(WORKFLOW_NAME)
                .majorVersion(WORKFLOW_MAJOR_VERSION)
                .minorVersion(WORKFLOW_MINOR_VERSION)
                .build();
    }

    private static byte[] serializeArguments(LaunchWorkflowRequest payload) {
        return JSON_BINDING.serialize(payload).getBytes(StandardCharsets.UTF_8);
    }
}
