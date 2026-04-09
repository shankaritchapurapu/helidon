/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.oracle.pic.workflow.client.v1.model.LaunchWorkflowArguments;
import com.oracle.pic.workflow.client.v1.model.WorkflowDefinitionId;
import com.oracle.pic.workflow.client.v1.model.WorkflowInstance;
import com.oracle.pic.workflow.client.v1.model.WorkflowStatus;
import com.oracle.pic.workflow.worker.WorkflowClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkflowIntegrationFlowTest {

    @Test
    void applicationServiceDemonstratesMainWorkflowFlow() {
        CapturingWorkflowClient workflowClient = new CapturingWorkflowClient();
        ProvisioningWorkflowService service = new ProvisioningWorkflowService(workflowClient.client());

        String workflowInstanceId = service.launch(new CreateInstanceRequest("app-instance",
                                                                             "VM.Standard3.Flex",
                                                                             "ocid1.compartment.oc1..example"));
        WorkflowStatus status = service.status(workflowInstanceId);

        assertEquals("wf-123", workflowInstanceId, "Service should return the launched workflow instance id");
        assertEquals(WorkflowStatus.Running, status, "Service should read the current workflow status");

        LaunchWorkflowArguments launchArguments = workflowClient.launchArguments();
        assertNotNull(launchArguments, "Launch request should be captured");
        assertEquals("instance-create", launchArguments.getWorkflowDefinitionId().getName(),
                     "Workflow id should come from application code");
        assertEquals(3, launchArguments.getWorkflowDefinitionId().getMajorVersion(),
                     "Workflow major version should come from application code");
        assertEquals(1, launchArguments.getWorkflowDefinitionId().getMinorVersion(),
                     "Workflow minor version should come from application code");
        assertEquals("{\"instanceName\":\"app-instance\",\"shape\":\"VM.Standard3.Flex\","
                             + "\"compartmentId\":\"ocid1.compartment.oc1..example\"}",
                     new String(launchArguments.getWorkflowArguments(), StandardCharsets.UTF_8),
                     "Workflow payload should be serialized from the application request");
        assertEquals("wf-123", workflowClient.lastRequestedWorkflowInstanceId(),
                     "Status lookup should use the launched workflow instance id");
    }

    private record CreateInstanceRequest(String instanceName, String shape, String compartmentId) {
    }

    private static final class ProvisioningWorkflowService {
        private final WorkflowClient workflowClient;

        private ProvisioningWorkflowService(WorkflowClient workflowClient) {
            this.workflowClient = workflowClient;
        }

        String launch(CreateInstanceRequest request) {
            LaunchWorkflowArguments launchRequest = LaunchWorkflowArguments.builder()
                    .workflowDefinitionId(WorkflowDefinitionId.builder()
                                                 .name("instance-create")
                                                 .majorVersion(3)
                                                 .minorVersion(1)
                                                 .build())
                    .workflowArguments(("{\"instanceName\":\"" + request.instanceName()
                            + "\",\"shape\":\"" + request.shape()
                            + "\",\"compartmentId\":\"" + request.compartmentId() + "\"}")
                            .getBytes(StandardCharsets.UTF_8))
                    .build();
            WorkflowInstance workflowInstance = workflowClient.launchWorkflow(launchRequest);
            return workflowInstance.getId();
        }

        WorkflowStatus status(String workflowInstanceId) {
            return workflowClient.getWorkflowInstance(workflowInstanceId).getStatus();
        }
    }

    private static final class CapturingWorkflowClient {
        private final AtomicReference<LaunchWorkflowArguments> launchArguments = new AtomicReference<>();
        private final AtomicReference<String> requestedWorkflowInstanceId = new AtomicReference<>();
        private final WorkflowClient client = (WorkflowClient) Proxy.newProxyInstance(
                WorkflowClient.class.getClassLoader(),
                new Class<?>[]{WorkflowClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDomainId" -> "compute-control-plane";
                    case "launchWorkflow" -> {
                        launchArguments.set((LaunchWorkflowArguments) args[0]);
                        yield WorkflowInstance.builder()
                                .id("wf-123")
                                .status(WorkflowStatus.Created)
                                .workflowDefinitionId(workflowDefinitionId())
                                .build();
                    }
                    case "getWorkflowInstance" -> {
                        requestedWorkflowInstanceId.set((String) args[0]);
                        yield WorkflowInstance.builder()
                                .id((String) args[0])
                                .status(WorkflowStatus.Running)
                                .workflowDefinitionId(workflowDefinitionId())
                                .build();
                    }
                    case "toString" -> "CapturingWorkflowClient";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                });

        private WorkflowClient client() {
            return client;
        }

        private LaunchWorkflowArguments launchArguments() {
            return launchArguments.get();
        }

        private String lastRequestedWorkflowInstanceId() {
            return requestedWorkflowInstanceId.get();
        }

        private static WorkflowDefinitionId workflowDefinitionId() {
            return WorkflowDefinitionId.builder()
                    .name("instance-create")
                    .majorVersion(3)
                    .minorVersion(1)
                    .build();
        }
    }
}
